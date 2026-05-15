package dev.ktcloud.black.common.util.backoff

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class CalcBackoffTest {
    @Test
    fun `returns time within expected range for SECONDS unit`() {
        val before = LocalDateTime.now()

        val result = getNextRetryDateTime(retryCount = 3, baseTime = 2, timeUnit = TimeUnit.SECONDS)

        val deltaSeconds = Duration.between(before, result).seconds
        // baseTime=2, retry=3 -> max=16; randomDelay in [2, 16+1)
        assertTrue(deltaSeconds in 1..18) { "Expected within 1..18s, was $deltaSeconds" }
    }

    @Test
    fun `returns time in the future for MINUTES unit`() {
        val before = LocalDateTime.now()

        val result = getNextRetryDateTime(retryCount = 1, baseTime = 2, timeUnit = TimeUnit.MINUTES)

        assertTrue(result.isAfter(before))
    }

    @Test
    fun `throws IllegalArgumentException for unsupported time unit`() {
        assertThrows(IllegalArgumentException::class.java) {
            getNextRetryDateTime(retryCount = 1, baseTime = 2, timeUnit = TimeUnit.MILLISECONDS)
        }
    }
}
