package dev.ktcloud.black.client.redis.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.redisson.api.RBucket
import org.redisson.api.RedissonClient
import java.time.Duration

class IdempotentEventProcessorTest {
    private val faker = Faker()
    private val distributedLock = mockk<DistributedLock>()
    private val redisson = mockk<RedissonClient>()
    private val sut = IdempotentEventProcessor(distributedLock, redisson)

    @Test
    fun `withIdempotencyProcess returns null when bucket already DONE`() {
        val key = faker.lorem().word()
        val bucket = mockk<RBucket<String>>()
        every { redisson.getBucket<String>("idempotent-processed$key") } returns bucket
        every { bucket.get() } returns "DONE"

        val result = sut.withIdempotencyProcess(key) { "should-not-run" }

        assertNull(result)
        verify(exactly = 0) { distributedLock.execute(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `withIdempotencyProcess runs func and marks bucket DONE on first call`() {
        val key = faker.lorem().word()
        val bucket = mockk<RBucket<String>>(relaxed = true)
        every { redisson.getBucket<String>("idempotent-processed$key") } returns bucket
        every { bucket.get() } returns null

        val funcSlot = slot<() -> Any?>()
        every {
            distributedLock.execute(eq(key), capture(funcSlot), any(), any(), any())
        } answers { funcSlot.captured.invoke() }

        val result = sut.withIdempotencyProcess(key, ttl = Duration.ofSeconds(10)) { 99 }

        assertEquals(99, result)
        verify { bucket.set("DONE", Duration.ofSeconds(10)) }
    }

    @Test
    fun `withIdempotencyProcess re-checks bucket inside lock to avoid double-run`() {
        val key = faker.lorem().word()
        val bucket = mockk<RBucket<String>>(relaxed = true)
        every { redisson.getBucket<String>("idempotent-processed$key") } returns bucket
        // First check (outside lock) returns null; second check (inside lock) returns DONE
        every { bucket.get() } returnsMany listOf(null, "DONE")

        val funcSlot = slot<() -> Any?>()
        every {
            distributedLock.execute(eq(key), capture(funcSlot), any(), any(), any())
        } answers { funcSlot.captured.invoke() }

        var ran = false
        val result = sut.withIdempotencyProcess(key) {
            ran = true
            "ran"
        }

        assertNull(result)
        assertEquals(false, ran)
        verify(exactly = 0) { bucket.set(any<String>(), any()) }
    }
}
