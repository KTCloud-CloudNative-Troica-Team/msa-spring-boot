package dev.ktcloud.black.inventory.adapter.infrastructure.redis

import dev.ktcloud.black.inventory.adapter.configuration.redis.RedisConfig
import dev.ktcloud.black.inventory.domain.exception.InventoryException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript

class InventoryRedisCommandAdapterTest {
    private val template = mockk<RedisTemplate<String, String>>()
    private val decreaseScript = mockk<RedisScript<Long>>()
    private val increaseScript = mockk<RedisScript<Long>>()
    private val setScript = mockk<RedisScript<Long>>()
    private val sut = InventoryRedisCommandAdapter(template, decreaseScript, increaseScript, setScript)

    @Test
    fun `decrease returns updated quantity`() {
        every { template.execute(decreaseScript, listOf("inventory:1"), "3", "10") } returns 7L

        val result = sut.decrease(inventoryId = 1, amount = 3, eventId = 10)

        assertEquals(7, result)
    }

    @Test
    fun `decrease translates not-enough error code into exception`() {
        every { template.execute(decreaseScript, any<List<String>>(), *anyVararg()) } returns
            RedisConfig.InventoryScriptError.INVENTORY_NOT_ENOUGH.errorCode.toLong()

        assertThrows(InventoryException.InventoryNotEnough::class.java) {
            sut.decrease(inventoryId = 1, amount = 99, eventId = 10)
        }
    }

    @Test
    fun `setInventoryQuantity translates stale error code into exception`() {
        every { template.execute(setScript, any<List<String>>(), *anyVararg()) } returns
            RedisConfig.InventoryScriptError.INVENTORY_DATA_STALE.errorCode.toLong()

        assertThrows(InventoryException.InventoryDataStale::class.java) {
            sut.setInventoryQuantity(inventoryId = 1, quantity = 5, eventId = 10)
        }
    }
}
