package dev.ktcloud.black.common.util.ratelimit

import io.github.bucket4j.BucketConfiguration
import java.time.Duration

data class RateLimitPolicy(
    val capacity: Long,
    val refillTokens: Long,
    val refillPeriod: Duration,
) {
    init {
        require(capacity > 0) { "capacity must be > 0, was $capacity" }
        require(refillTokens > 0) { "refillTokens must be > 0, was $refillTokens" }
        require(!refillPeriod.isZero && !refillPeriod.isNegative) {
            "refillPeriod must be positive, was $refillPeriod"
        }
    }

    fun toBucketConfiguration(): BucketConfiguration =
        BucketConfiguration.builder()
            .addLimit { bandwidth ->
                bandwidth.capacity(capacity).refillGreedy(refillTokens, refillPeriod)
            }
            .build()
}
