package dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.configuration

import dev.ktcloud.black.common.util.ratelimit.RateLimitPolicy
import dev.ktcloud.black.common.util.ratelimit.TokenBucketRateLimiter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Duration

@ConfigurationProperties(prefix = "rate-limit")
data class RateLimitProperties(
    val capacity: Long = 40,
    val refillTokens: Long = 20,
    val refillPeriod: Duration = Duration.ofSeconds(1),
    val keyPrefix: String = "admin-gateway:",
) {
    fun toPolicy(): RateLimitPolicy = RateLimitPolicy(capacity, refillTokens, refillPeriod)
}

@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties(RateLimitProperties::class)
class RateLimiterConfig {
    @Bean
    fun ipRateLimitKeyResolver(): IpRateLimitKeyResolver = IpRateLimitKeyResolver()
}

class IpRateLimitKeyResolver {
    fun resolve(exchange: ServerWebExchange): String {
        val forwarded = exchange.request.headers.getFirst("X-Forwarded-For")
            ?.substringBefore(',')
            ?.trim()
        val remote = exchange.request.remoteAddress?.address?.hostAddress
        return forwarded ?: remote ?: "anonymous"
    }
}

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class TokenBucketRateLimitWebFilter(
    private val rateLimiter: TokenBucketRateLimiter,
    private val properties: RateLimitProperties,
    private val keyResolver: IpRateLimitKeyResolver,
) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val key = "${properties.keyPrefix}${keyResolver.resolve(exchange)}"
        return rateLimiter.tryConsumeReactive(key, properties.toPolicy())
            .flatMap { result ->
                val response = exchange.response
                response.headers.set("X-RateLimit-Remaining", result.remainingTokens.toString())
                if (result.allowed) {
                    chain.filter(exchange)
                } else {
                    val retryAfterSeconds = result.retryAfter.seconds.coerceAtLeast(1)
                    response.statusCode = HttpStatus.TOO_MANY_REQUESTS
                    response.headers.set("Retry-After", retryAfterSeconds.toString())
                    response.setComplete()
                }
            }
    }
}
