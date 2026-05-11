package dev.ktcloud.black.auth.application.service.jwt

import dev.ktcloud.black.auth.domain.exception.AuthException
import dev.ktcloud.black.user.domain.vo.UserDetail
import dev.ktcloud.black.user.domain.vo.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtResolver(
    @Value("\${jwt.secret}") private val secretKey: String
) {
    private val signingKey: SecretKey = Keys.hmacShaKeyFor(secretKey.toByteArray())
    fun extractClaims(token: String): Claims {
        val claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload

        return claims
    }

    fun validateToken(token: String): UserDetail {
        val claims = extractClaims(token)

        if (claims.expiration.time - Date().time < 0) throw AuthException.ExpiredAccessToken()

        return UserDetail(
            id = UUID.fromString(claims.subject),
            role = UserRole.valueOf(claims["role"] as String),
            email = claims["email"] as String,
            name = claims["name"] as String,
        )
    }
}