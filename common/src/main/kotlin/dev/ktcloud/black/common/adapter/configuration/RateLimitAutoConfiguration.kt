package dev.ktcloud.black.common.adapter.configuration

import dev.ktcloud.black.common.util.ratelimit.TokenBucketRateLimiter
import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(LettuceBasedProxyManager::class, RedisClient::class)
@ConditionalOnProperty(prefix = "spring.data.redis", name = ["host"])
class RateLimitAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = ["rateLimitRedisClient"])
    fun rateLimitRedisClient(
        @Value("\${spring.data.redis.host}") host: String,
        @Value("\${spring.data.redis.port:6379}") port: Int,
        @Value("\${spring.data.redis.password:}") password: String,
        @Value("\${spring.data.redis.database:0}") database: Int,
    ): RedisClient {
        val uri = RedisURI.Builder.redis(host, port)
            .withDatabase(database)
            .apply { if (password.isNotEmpty()) withPassword(password.toCharArray()) }
            .build()
        return RedisClient.create(uri)
    }

    @Bean
    @ConditionalOnMissingBean(ProxyManager::class)
    fun rateLimitProxyManager(rateLimitRedisClient: RedisClient): ProxyManager<ByteArray> =
        Bucket4jLettuce.casBasedBuilder(rateLimitRedisClient).build()

    @Bean
    @ConditionalOnMissingBean
    fun tokenBucketRateLimiter(proxyManager: ProxyManager<ByteArray>): TokenBucketRateLimiter =
        TokenBucketRateLimiter(proxyManager)
}
