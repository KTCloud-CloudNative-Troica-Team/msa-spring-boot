package dev.ktcloud.black.auth.application.service

import dev.ktcloud.black.auth.application.dto.JwtTokenDto
import dev.ktcloud.black.auth.application.port.cache.outbound.AuthCacheCommandOutbound
import dev.ktcloud.black.auth.application.port.cache.outbound.AuthCacheQueryOutbound
import dev.ktcloud.black.auth.application.port.inbound.SignInCommand
import dev.ktcloud.black.auth.application.port.inbound.TokenRefreshCommand
import dev.ktcloud.black.auth.application.service.jwt.JwtGenerator
import dev.ktcloud.black.auth.application.service.jwt.JwtResolver
import dev.ktcloud.black.auth.domain.exception.AuthException
import dev.ktcloud.black.user.application.dto.UserDto
import dev.ktcloud.black.user.application.port.outbound.UserQueryOutboundPort
import dev.ktcloud.black.user.domain.exception.UserException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AuthCommandService(
    private val jwtResolver: JwtResolver,
    private val jwtGenerator: JwtGenerator,
    private val authCacheQueryOutbound: AuthCacheQueryOutbound,
    private val authCacheCommandOutbound: AuthCacheCommandOutbound,
    private val userQueryOutboundPort: UserQueryOutboundPort,
    private val passwordEncoder: PasswordEncoder
): TokenRefreshCommand, SignInCommand {
    @Transactional
    override fun refresh(command: TokenRefreshCommand.In): TokenRefreshCommand.Out  {
        val userId = jwtResolver.extractClaims(command.refreshToken).subject

        val savedToken = authCacheQueryOutbound.getRefreshToken(userId)
        if (savedToken != command.refreshToken) {
            authCacheCommandOutbound.deleteRefreshToken(userId)
            throw AuthException.RefreshTokenFail()
        }

        val user = userQueryOutboundPort.findById(UUID.fromString(userId))

        val newToken = jwtGenerator.generate(user)

        authCacheCommandOutbound.saveRefreshToken(user.id.toString(), newToken.second)

        return TokenRefreshCommand.Out(
            token = JwtTokenDto(
                accessToken = newToken.first,
                refreshToken = newToken.second
            ),
            user = UserDto.from(user)
        )
    }

    @Transactional
    override fun signIn(command: SignInCommand.In): SignInCommand.Out {
        val user = userQueryOutboundPort.findByEmail(command.email)

        if (!passwordEncoder.matches(command.password, user.password))
            throw UserException.UserNotFoundException()

        val (accessToken, refreshToken) = jwtGenerator.generate(user)

        authCacheCommandOutbound.saveRefreshToken(
            userId = user.id.toString(),
            refreshToken = refreshToken,
        )

        return SignInCommand.Out(
            token = JwtTokenDto(
                accessToken = accessToken,
                refreshToken = refreshToken
            ),
            user = UserDto.from(user)
        )
    }
}