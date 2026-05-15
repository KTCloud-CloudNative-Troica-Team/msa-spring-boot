package dev.ktcloud.black.common.util.ratelimit

import io.github.bucket4j.ConsumptionProbe
import java.time.Duration

data class RateLimitResult(
    val allowed: Boolean,
    val remainingTokens: Long,
    val retryAfter: Duration,
) {
    companion object {
        fun from(probe: ConsumptionProbe): RateLimitResult = RateLimitResult(
            allowed = probe.isConsumed,
            remainingTokens = probe.remainingTokens,
            retryAfter = Duration.ofNanos(probe.nanosToWaitForRefill),
        )
    }
}
