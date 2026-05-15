package dev.ktcloud.black.inventory.adapter.infrastructure

import dev.ktcloud.black.inventory.adapter.configuration.redis.RedisConfig
import dev.ktcloud.black.inventory.application.port.cache.outbound.InventoryCacheCommandOutboundPort
import dev.ktcloud.black.inventory.application.port.cache.outbound.InventoryCacheQueryOutboundPort
import dev.ktcloud.black.inventory.application.port.state.outbound.InventoryStateQueryOutboundPort
import dev.ktcloud.black.inventory.domain.entity.InventoryDomainEntity
import dev.ktcloud.black.inventory.domain.exception.InventoryException
import dev.ktcloud.black.inventory.event.application.port.outbound.InventoryEventQueryOutboundPort
import dev.ktcloud.black.inventory.event.domain.entity.InventoryEventDomainEntity
import dev.ktcloud.black.inventory.event.domain.vo.InventoryEventType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LoadCacheSyncedInventoryPersistenceAdapterTest {
    private val state = mockk<InventoryStateQueryOutboundPort>()
    private val cacheQuery = mockk<InventoryCacheQueryOutboundPort>()
    private val cacheCommand = mockk<InventoryCacheCommandOutboundPort>()
    private val eventQuery = mockk<InventoryEventQueryOutboundPort>()
    private val sut = LoadCacheSyncedInventoryPersistenceAdapter(state, cacheQuery, cacheCommand, eventQuery)

    private fun inventory(id: Long = 1, qty: Int = 0) =
        InventoryDomainEntity(id = id, productId = "p", skuCode = "s", _quantity = qty)

    @Test
    fun `loadInventory returns cached quantity when present`() {
        every { state.fetch(1L) } returns inventory(1, 0)
        every { cacheQuery.fetchInventory(1L) } returns 8

        val result = sut.loadInventory(1L)

        assertEquals(8, result.quantity)
    }

    @Test
    fun `loadInventory throws when cache missing`() {
        every { state.fetch(1L) } returns inventory(1, 0)
        every { cacheQuery.fetchInventory(1L) } returns RedisConfig.InventoryScriptError.NO_CACHED_INVENTORY_FOUND.errorCode

        assertThrows(InventoryException.NoCachedInventoryFound::class.java) { sut.loadInventory(1L) }
    }

    @Test
    fun `loadCacheSyncedInventory rebuilds cache from state plus pending events`() {
        every { state.fetch(1L) } returns inventory(1, 5)
        every { cacheQuery.fetchInventory(1L) } returns RedisConfig.InventoryScriptError.NO_CACHED_INVENTORY_FOUND.errorCode
        every { eventQuery.fetchUnprocessedEvents(1L) } returns listOf(
            InventoryEventDomainEntity(id = 10, inventoryId = 1, amount = -2, eventType = InventoryEventType.DECREMENT)
        )
        every { cacheCommand.setInventoryQuantity(1L, 3, 10L) } returns 3

        val result = sut.loadCacheSyncedInventory(1L)

        assertEquals(3, result.quantity)
    }

    @Test
    fun `loadCacheSyncedInventory falls back to cache reread when cache becomes stale during rebuild`() {
        every { state.fetch(1L) } returns inventory(1, 5)
        every { cacheQuery.fetchInventory(1L) } returnsMany listOf(
            RedisConfig.InventoryScriptError.NO_CACHED_INVENTORY_FOUND.errorCode,
            12
        )
        every { eventQuery.fetchUnprocessedEvents(1L) } returns emptyList()
        every { cacheCommand.setInventoryQuantity(1L, 5, 0L) } returns RedisConfig.InventoryScriptError.INVENTORY_DATA_STALE.errorCode

        val result = sut.loadCacheSyncedInventory(1L)

        assertEquals(12, result.quantity)
    }
}
