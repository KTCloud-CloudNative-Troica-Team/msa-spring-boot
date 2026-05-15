package dev.ktcloud.black.auth.adapter.infrastructure.redis

import dev.ktcloud.black.auth.application.service.jwt.JwtResolver
import dev.ktcloud.black.auth.domain.exception.AuthException
import io.jsonwebtoken.Claims
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.util.Date
import java.util.concurrent.TimeUnit

class AuthCacheRedisCommandAdapterTest {
    private val template = mockk<RedisTemplate<String, String>>(relaxed = true)
    private val ops = mockk<ValueOperations<String, String>>(relaxed = true)
    private val resolver = mockk<JwtResolver>()
    private val sut = AuthCacheRedisCommandAdapter(template, resolver)

    @Test
    fun `saveRefreshToken stores value with TTL equal to remaining lifetime`() {
        val claims = mockk<Claims>()
        every { claims.expiration } returns Date(System.currentTimeMillis() + 60_000)
        every { resolver.extractClaims("REF") } returns claims
        every { template.opsForValue() } returns ops

        sut.saveRefreshToken("u-1", "REF")

        verify {
            ops.set(
                "auth-refresh-token:u-1",
                "REF",
                match<Long> { it in 1L..60_000L },
                TimeUnit.MILLISECONDS
            )
        }
    }

    @Test
    fun `saveRefreshToken throws when refresh has effectively expired`() {
        val claims = mockk<Claims>()
        every { claims.expiration } returns Date(System.currentTimeMillis() - 1_000)
        every { resolver.extractClaims("REF") } returns claims

        assertThrows(AuthException.ExpiredRefreshToken::class.java) {
            sut.saveRefreshToken("u-1", "REF")
        }
    }

    @Test
    fun `deleteRefreshToken removes key`() {
        sut.deleteRefreshToken("u-2")

        verify { template.delete("auth-refresh-token:u-2") }
    }
}
