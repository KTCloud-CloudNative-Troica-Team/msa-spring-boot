package dev.ktcloud.black.auth.application.service.jwt

import dev.ktcloud.black.user.domain.entity.UserDomainEntity
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtGenerator(
    @Value("\${jwt.secret}") private val secretKey: String
) {
    companion object {
        const val ACCESS_TOKEN_DURABILITY = 1000 * 60 * 30 * 24 * 7
        const val REFRESH_TOKEN_DURABILITY = 1000 * 60 * 60 * 24 * 7
    }

    fun generate(user: UserDomainEntity): Pair<String, String> {
        val key: SecretKey = Keys.hmacShaKeyFor(secretKey.toByteArray())

        val now = Date()

        val accessToken = Jwts.builder()
            .subject(user.id.toString())
            .claims(
                mapOf(
                    "role" to user.role,
                    "email" to user.email,
                    "name" to user.name,
                )
            )
            .issuedAt(now)
            .expiration(Date(now.time + ACCESS_TOKEN_DURABILITY))
            .signWith(key)
            .compact()


        val refreshToken = Jwts.builder()
            .subject(user.id.toString())
            .issuedAt(now)
            .expiration(Date(now.time + REFRESH_TOKEN_DURABILITY))
            .signWith(key)
            .compact()

        return accessToken to refreshToken
    }
}