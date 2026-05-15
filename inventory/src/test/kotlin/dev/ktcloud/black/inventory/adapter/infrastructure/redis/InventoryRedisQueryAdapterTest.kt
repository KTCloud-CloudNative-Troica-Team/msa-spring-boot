package dev.ktcloud.black.inventory.adapter.infrastructure.redis

import dev.ktcloud.black.inventory.adapter.configuration.redis.RedisConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.RedisTemplate

class InventoryRedisQueryAdapterTest {
    private val template = mockk<RedisTemplate<String, String>>()
    private val hashOps = mockk<HashOperations<String, String, String>>()
    private val sut = InventoryRedisQueryAdapter(template)

    @Test
    fun `fetchInventory returns parsed quantity when present`() {
        every { template.opsForHash<String, String>() } returns hashOps
        every { hashOps.get("inventory:1", "quantity") } returns "42"

        assertEquals(42, sut.fetchInventory(1L))
    }

    @Test
    fun `fetchInventory returns NO_CACHED_INVENTORY error code when missing`() {
        every { template.opsForHash<String, String>() } returns hashOps
        every { hashOps.get("inventory:1", "quantity") } returns null

        assertEquals(
            RedisConfig.InventoryScriptError.NO_CACHED_INVENTORY_FOUND.errorCode,
            sut.fetchInventory(1L)
        )
    }
}
