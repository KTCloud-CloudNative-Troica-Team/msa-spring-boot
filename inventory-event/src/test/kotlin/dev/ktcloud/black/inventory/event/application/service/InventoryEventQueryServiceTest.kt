package dev.ktcloud.black.inventory.event.application.service

import dev.ktcloud.black.inventory.event.application.port.inbound.FetchUnprocessedInventoryEventsQuery
import dev.ktcloud.black.inventory.event.application.port.outbound.InventoryEventQueryOutboundPort
import dev.ktcloud.black.inventory.event.domain.entity.InventoryEventDomainEntity
import dev.ktcloud.black.inventory.event.domain.vo.InventoryEventType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InventoryEventQueryServiceTest {
    private val queryPort = mockk<InventoryEventQueryOutboundPort>()
    private val sut = InventoryEventQueryService(queryPort)

    @Test
    fun `fetchUnprocessed maps domain entities to Out`() {
        val event = InventoryEventDomainEntity(
            id = 7,
            inventoryId = 12,
            amount = 4,
            eventType = InventoryEventType.INCREMENT
        )
        every { queryPort.fetchUnprocessedEvents(12) } returns listOf(event)

        val result = sut.fetchUnprocessed(FetchUnprocessedInventoryEventsQuery.In(inventoryId = 12))

        assertEquals(1, result.size)
        assertEquals(7, result[0].id)
        assertEquals(InventoryEventType.INCREMENT, result[0].eventType)
    }

    @Test
    fun `fetchUnprocessed returns empty list when no events exist`() {
        every { queryPort.fetchUnprocessedEvents(99) } returns emptyList()

        val result = sut.fetchUnprocessed(FetchUnprocessedInventoryEventsQuery.In(inventoryId = 99))

        assertTrue(result.isEmpty())
    }
}
