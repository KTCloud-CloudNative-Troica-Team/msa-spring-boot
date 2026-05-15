package dev.ktcloud.black.common.util.time

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId

class MicrosecondTruncatingClockTest {
    @Test
    fun `instant truncates nanoseconds to microsecond precision`() {
        val instant = MicrosecondTruncatingClock.Instance.instant()

        assertEquals(0, instant.nano % 1_000)
    }

    @Test
    fun `withZone returns clock with new zone`() {
        val tokyo = ZoneId.of("Asia/Tokyo")

        val clock = MicrosecondTruncatingClock.Instance.withZone(tokyo)

        assertEquals(tokyo, clock.zone)
    }

    @Test
    fun `now returns time truncated to microseconds`() {
        val time = now()

        assertEquals(0, time.nano % 1_000)
    }
}
