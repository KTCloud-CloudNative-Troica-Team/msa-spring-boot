package dev.ktcloud.black.auth.application.service.jwt

import dev.ktcloud.black.user.domain.entity.UserDomainEntity
import dev.ktcloud.black.user.domain.vo.UserRole
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID

class JwtGeneratorResolverTest {
    private val secret = "test-secret-test-secret-test-secret-test-secret-12345"
    private val generator = JwtGenerator(secret)
    private val resolver = JwtResolver(secret)

    private val user = UserDomainEntity(
        id = UUID.randomUUID(), role = UserRole.USER, email = "u@x", password = "p", name = "n"
    )

    @Test
    fun `generate produces an access token resolvable into a UserDetail`() {
        val (access, _) = generator.generate(user)

        val resolved = resolver.validateToken(access)

        assertEquals(user.id, resolved.id)
        assertEquals(user.email, resolved.email)
        assertEquals(UserRole.USER, resolved.role)
    }

    @Test
    fun `generate produces distinct refresh and access tokens`() {
        val (access, refresh) = generator.generate(user)

        assertNotEquals(access, refresh)
    }

    @Test
    fun `validateToken rejects an already-expired token`() {
        val key = Keys.hmacShaKeyFor(secret.toByteArray())
        val expired = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claims(mapOf("role" to "USER", "email" to "x", "name" to "n"))
            .issuedAt(Date(System.currentTimeMillis() - 60_000))
            .expiration(Date(System.currentTimeMillis() - 1_000))
            .signWith(key)
            .compact()

        assertThrows(ExpiredJwtException::class.java) {
            resolver.validateToken(expired)
        }
    }
}
