package dev.ktcloud.black.common.util.ratelimit

import io.github.bucket4j.distributed.proxy.ProxyManager
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets

class TokenBucketRateLimiter(
    private val proxyManager: ProxyManager<ByteArray>,
    private val keyPrefix: String = DEFAULT_KEY_PREFIX,
) {
    fun tryConsume(key: String, policy: RateLimitPolicy, tokens: Long = 1): RateLimitResult {
        val bucket = proxyManager.builder().build(encodeKey(key)) { policy.toBucketConfiguration() }
        val probe = bucket.tryConsumeAndReturnRemaining(tokens)
        return RateLimitResult.from(probe)
    }

    fun tryConsumeReactive(
        key: String,
        policy: RateLimitPolicy,
        tokens: Long = 1,
    ): Mono<RateLimitResult> {
        if (!proxyManager.isAsyncModeSupported) {
            return Mono.fromCallable { tryConsume(key, policy, tokens) }
        }
        val asyncBucket = proxyManager
            .asAsync()
            .builder()
            .build(encodeKey(key)) {
                java.util.concurrent.CompletableFuture.completedFuture(policy.toBucketConfiguration())
            }
        return Mono.fromFuture(asyncBucket.tryConsumeAndReturnRemaining(tokens))
            .map(RateLimitResult::from)
    }

    private fun encodeKey(key: String): ByteArray =
        "$keyPrefix$key".toByteArray(StandardCharsets.UTF_8)

    companion object {
        const val DEFAULT_KEY_PREFIX = "rate-limit:"
    }
}
