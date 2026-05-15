package dev.ktcloud.black.inventory.event.application.service

import dev.ktcloud.black.inventory.event.application.port.inbound.CreateInventoryEventCommand
import dev.ktcloud.black.inventory.event.application.port.inbound.SetStatusProcessedCommand
import dev.ktcloud.black.inventory.event.application.port.outbound.InventoryEventCommandOutboundPort
import dev.ktcloud.black.inventory.event.application.port.outbound.InventoryEventQueryOutboundPort
import dev.ktcloud.black.inventory.event.domain.entity.InventoryEventDomainEntity
import dev.ktcloud.black.inventory.event.domain.exception.InventoryEventException
import dev.ktcloud.black.inventory.event.domain.vo.InventoryEventProcessStatus
import dev.ktcloud.black.inventory.event.domain.vo.InventoryEventType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class InventoryEventCommandServiceTest {
    private val faker = Faker()
    private val queryPort = mockk<InventoryEventQueryOutboundPort>()
    private val commandPort = mockk<InventoryEventCommandOutboundPort>(relaxed = true)
    private val sut = InventoryEventCommandService(queryPort, commandPort)

    @Test
    fun `create persists a valid increment event`() {
        val captured = slot<InventoryEventDomainEntity>()
        every { commandPort.save(capture(captured)) } returns mockk()

        sut.create(
            CreateInventoryEventCommand.In(
                inventoryId = faker.number().positive().toLong(),
                amount = 5,
                eventType = InventoryEventType.INCREMENT
            )
        )

        assertEquals(5, captured.captured.amount)
        assertEquals(InventoryEventType.INCREMENT, captured.captured.eventType)
    }

    @Test
    fun `create rejects increment event with negative amount`() {
        assertThrows(InventoryEventException.InventoryEventInvalid::class.java) {
            sut.create(
                CreateInventoryEventCommand.In(
                    inventoryId = 1,
                    amount = -3,
                    eventType = InventoryEventType.INCREMENT
                )
            )
        }
    }

    @Test
    fun `setStatusProcessed marks fetched events as processed and saves them`() {
        val ids = listOf(1L, 2L)
        val events = listOf(
            InventoryEventDomainEntity(id = 1, inventoryId = 10, amount = 1, eventType = InventoryEventType.INCREMENT),
            InventoryEventDomainEntity(id = 2, inventoryId = 10, amount = -1, eventType = InventoryEventType.DECREMENT)
        )
        every { queryPort.fetchAll(ids) } returns events

        sut.setStatusProcessed(SetStatusProcessedCommand.In(ids))

        events.forEach { assertEquals(InventoryEventProcessStatus.PROCESSED, it.processStatus) }
        verify { commandPort.saveAll(events) }
    }
}
