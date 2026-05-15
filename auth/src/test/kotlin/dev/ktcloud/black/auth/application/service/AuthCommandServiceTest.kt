package dev.ktcloud.black.auth.application.service

import dev.ktcloud.black.auth.application.port.cache.outbound.AuthCacheCommandOutbound
import dev.ktcloud.black.auth.application.port.cache.outbound.AuthCacheQueryOutbound
import dev.ktcloud.black.auth.application.port.inbound.SignInCommand
import dev.ktcloud.black.auth.application.port.inbound.TokenRefreshCommand
import dev.ktcloud.black.auth.application.service.jwt.JwtGenerator
import dev.ktcloud.black.auth.application.service.jwt.JwtResolver
import dev.ktcloud.black.auth.domain.exception.AuthException
import dev.ktcloud.black.user.application.port.outbound.UserQueryOutboundPort
import dev.ktcloud.black.user.domain.entity.UserDomainEntity
import dev.ktcloud.black.user.domain.exception.UserException
import dev.ktcloud.black.user.domain.vo.UserRole
import io.jsonwebtoken.Claims
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

class AuthCommandServiceTest {
    private val jwtResolver = mockk<JwtResolver>()
    private val jwtGenerator = mockk<JwtGenerator>()
    private val cacheQuery = mockk<AuthCacheQueryOutbound>()
    private val cacheCommand = mockk<AuthCacheCommandOutbound>(relaxed = true)
    private val userQuery = mockk<UserQueryOutboundPort>()
    private val encoder = mockk<PasswordEncoder>()
    private val sut = AuthCommandService(jwtResolver, jwtGenerator, cacheQuery, cacheCommand, userQuery, encoder)

    private val userId = UUID.randomUUID()
    private val user = UserDomainEntity(
        id = userId, role = UserRole.USER, email = "u@x", password = "ENC", name = "n"
    )

    @Test
    fun `signIn issues tokens and stores refresh token on valid credentials`() {
        every { userQuery.findByEmail("u@x") } returns user
        every { encoder.matches("plain", "ENC") } returns true
        every { jwtGenerator.generate(user) } returns ("ACC" to "REF")

        val out = sut.signIn(SignInCommand.In(email = "u@x", password = "plain"))

        assertEquals("ACC", out.token.accessToken)
        assertEquals("REF", out.token.refreshToken)
        verify { cacheCommand.saveRefreshToken(userId.toString(), "REF") }
    }

    @Test
    fun `signIn throws when password mismatches`() {
        every { userQuery.findByEmail("u@x") } returns user
        every { encoder.matches(any(), any()) } returns false

        assertThrows(UserException.UserNotFoundException::class.java) {
            sut.signIn(SignInCommand.In(email = "u@x", password = "wrong"))
        }
    }

    @Test
    fun `refresh issues new tokens when stored refresh matches`() {
        val claims = mockk<Claims>()
        every { claims.subject } returns userId.toString()
        every { jwtResolver.extractClaims("OLD-REF") } returns claims
        every { cacheQuery.getRefreshToken(userId.toString()) } returns "OLD-REF"
        every { userQuery.findById(userId) } returns user
        every { jwtGenerator.generate(user) } returns ("NEW-ACC" to "NEW-REF")

        val out = sut.refresh(TokenRefreshCommand.In(refreshToken = "OLD-REF"))

        assertEquals("NEW-ACC", out.token.accessToken)
        verify { cacheCommand.saveRefreshToken(userId.toString(), "NEW-REF") }
    }

    @Test
    fun `refresh deletes stored token and throws when refresh mismatches`() {
        val claims = mockk<Claims>()
        every { claims.subject } returns userId.toString()
        every { jwtResolver.extractClaims(any()) } returns claims
        every { cacheQuery.getRefreshToken(userId.toString()) } returns "DIFFERENT"

        assertThrows(AuthException.RefreshTokenFail::class.java) {
            sut.refresh(TokenRefreshCommand.In(refreshToken = "STALE"))
        }
        verify { cacheCommand.deleteRefreshToken(userId.toString()) }
    }
}
