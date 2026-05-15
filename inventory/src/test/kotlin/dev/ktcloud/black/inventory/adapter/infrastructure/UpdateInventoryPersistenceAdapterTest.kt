package dev.ktcloud.black.inventory.adapter.infrastructure

import dev.ktcloud.black.inventory.application.port.cache.outbound.InventoryCacheCommandOutboundPort
import dev.ktcloud.black.inventory.event.application.port.outbound.InventoryEventCommandOutboundPort
import dev.ktcloud.black.inventory.event.domain.entity.InventoryEventDomainEntity
import dev.ktcloud.black.inventory.event.domain.vo.InventoryEventType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UpdateInventoryPersistenceAdapterTest {
    private val cache = mockk<InventoryCacheCommandOutboundPort>()
    private val eventPort = mockk<InventoryEventCommandOutboundPort>()
    private val sut = UpdateInventoryPersistenceAdapter(cache, eventPort)

    @Test
    fun `increase persists INCREMENT event then updates cache`() {
        val captured = slot<InventoryEventDomainEntity>()
        every { eventPort.save(capture(captured)) } answers {
            InventoryEventDomainEntity(
                id = 11,
                inventoryId = captured.captured.inventoryId,
                amount = captured.captured.amount,
                eventType = captured.captured.eventType
            )
        }
        every { cache.increase(7L, 4, 11L) } returns 14

        val result = sut.increase(7L, 4)

        assertEquals(14, result)
        assertEquals(InventoryEventType.INCREMENT, captured.captured.eventType)
    }

    @Test
    fun `decrease persists DECREMENT event then updates cache`() {
        val captured = slot<InventoryEventDomainEntity>()
        every { eventPort.save(capture(captured)) } answers {
            InventoryEventDomainEntity(
                id = 12,
                inventoryId = captured.captured.inventoryId,
                amount = captured.captured.amount,
                eventType = captured.captured.eventType
            )
        }
        every { cache.decrease(7L, 4, 12L) } returns 6

        val result = sut.decrease(7L, 4)

        assertEquals(6, result)
        assertEquals(InventoryEventType.DECREMENT, captured.captured.eventType)
    }
}
