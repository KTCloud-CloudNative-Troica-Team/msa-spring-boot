package dev.ktcloud.black.auth.adapter.infrastructure.redis

import dev.ktcloud.black.auth.domain.exception.AuthException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations

class AuthCacheRedisQueryAdapterTest {
    private val template = mockk<RedisTemplate<String, String>>()
    private val ops = mockk<ValueOperations<String, String>>()
    private val sut = AuthCacheRedisQueryAdapter(template)

    @Test
    fun `getRefreshToken returns stored value`() {
        every { template.opsForValue() } returns ops
        every { ops.get("auth-refresh-token:u-1") } returns "REF"

        assertEquals("REF", sut.getRefreshToken("u-1"))
    }

    @Test
    fun `getRefreshToken throws when no value present`() {
        every { template.opsForValue() } returns ops
        every { ops.get(any()) } returns null

        assertThrows(AuthException.ExpiredRefreshToken::class.java) {
            sut.getRefreshToken("missing")
        }
    }
}
