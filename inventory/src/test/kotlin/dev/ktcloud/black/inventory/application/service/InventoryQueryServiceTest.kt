package dev.ktcloud.black.inventory.application.service

import dev.ktcloud.black.client.redis.api.DistributedLock
import dev.ktcloud.black.inventory.application.port.inbound.query.LoadInventoryQuery
import dev.ktcloud.black.inventory.application.port.outbound.LoadCacheSyncedInventoryOutboundPort
import dev.ktcloud.black.inventory.application.port.state.outbound.InventoryStateQueryOutboundPort
import dev.ktcloud.black.inventory.domain.entity.InventoryDomainEntity
import dev.ktcloud.black.inventory.domain.exception.InventoryException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InventoryQueryServiceTest {
    private val distributedLock = mockk<DistributedLock>()
    private val loadAdapter = mockk<LoadCacheSyncedInventoryOutboundPort>()
    private val stateQuery = mockk<InventoryStateQueryOutboundPort>()
    private val sut = InventoryQueryService(distributedLock, loadAdapter, stateQuery)

    private fun bypassLock() {
        val funcSlot = slot<() -> Any?>()
        every {
            distributedLock.execute(any(), capture(funcSlot), any(), any(), any())
        } answers { funcSlot.captured.invoke() }
    }

    private fun inventory(id: Long, qty: Int = 0) =
        InventoryDomainEntity(id = id, productId = "p$id", skuCode = "s$id", _quantity = qty)

    @Test
    fun `load returns cached inventory directly when available`() {
        every { loadAdapter.loadInventory(7L) } returns inventory(7, 5)

        val out = sut.load(LoadInventoryQuery.In(id = 7))

        assertEquals(5, out.quantity)
        verify(exactly = 0) { distributedLock.execute(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `load rehydrates cache under lock when no cached inventory found`() {
        bypassLock()
        every { loadAdapter.loadInventory(7L) } throws InventoryException.NoCachedInventoryFound()
        every { loadAdapter.loadCacheSyncedInventory(7L) } returns inventory(7, 9)

        val out = sut.load(LoadInventoryQuery.In(id = 7))

        assertEquals(9, out.quantity)
    }

    @Test
    fun `fetchAll loads each inventory and maps results`() {
        bypassLock()
        every { stateQuery.fetchAll() } returns listOf(inventory(1, 0), inventory(2, 0))
        every { loadAdapter.loadInventory(1L) } returns inventory(1, 3)
        every { loadAdapter.loadInventory(2L) } returns inventory(2, 4)

        val out = sut.fetchAll()

        assertEquals(2, out.size)
        assertEquals(setOf(3, 4), out.map { it.quantity }.toSet())
    }
}
