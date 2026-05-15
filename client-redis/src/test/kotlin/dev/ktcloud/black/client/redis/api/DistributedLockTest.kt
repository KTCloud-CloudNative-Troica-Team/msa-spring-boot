package dev.ktcloud.black.client.redis.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class DistributedLockTest {
    private val faker = Faker()
    private val redisson = mockk<RedissonClient>()
    private val lock = mockk<RLock>(relaxed = true)
    private val sut = DistributedLock(redisson)

    @Test
    fun `execute runs func and releases lock when acquired`() {
        val key = faker.lorem().word()
        every { redisson.getLock(key) } returns lock
        every { lock.tryLock(any(), any(), any()) } returns true

        val result = sut.execute(key, func = { 42 })

        assertEquals(42, result)
        verify { lock.unlock() }
    }

    @Test
    fun `execute throws TimeoutException when lock not acquired`() {
        val key = faker.lorem().word()
        every { redisson.getLock(key) } returns lock
        every { lock.tryLock(any(), any(), any()) } returns false

        assertThrows(TimeoutException::class.java) {
            sut.execute(key, func = { "ignored" })
        }
        verify(exactly = 0) { lock.unlock() }
    }

    @Test
    fun `execute releases lock even when func throws`() {
        val key = faker.lorem().word()
        every { redisson.getLock(key) } returns lock
        every { lock.tryLock(any(), any(), any()) } returns true

        assertThrows(IllegalStateException::class.java) {
            sut.execute(key, func = { throw IllegalStateException("boom") })
        }
        verify { lock.unlock() }
    }

    @Test
    fun `executeNowOrFail wraps timeout into IllegalStateException`() {
        val key = faker.lorem().word()
        every { redisson.getLock(key) } returns lock
        every { lock.tryLock(0, 3L, TimeUnit.SECONDS) } returns false

        assertThrows(IllegalStateException::class.java) {
            sut.executeNowOrFail(key, func = { "x" })
        }
    }
}
