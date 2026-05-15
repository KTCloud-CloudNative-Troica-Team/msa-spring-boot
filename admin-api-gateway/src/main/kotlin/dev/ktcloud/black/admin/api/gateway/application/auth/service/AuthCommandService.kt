package dev.ktcloud.black.admin.api.gateway.application.auth.service

import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.configuration.AdminSecurityProperties
import dev.ktcloud.black.admin.api.gateway.application.auth.dto.JwtDto
import dev.ktcloud.black.admin.api.gateway.application.auth.dto.UserDto
import dev.ktcloud.black.admin.api.gateway.application.auth.port.inbound.SignInCommand
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.AuthServiceGrpcKt
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.SignInRequest
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AuthCommandService(
    @GrpcClient("auth-service")
    private val authServiceStub: AuthServiceGrpcKt.AuthServiceCoroutineStub,
    private val adminSecurityProperties: AdminSecurityProperties,
) : SignInCommand {
    @CircuitBreaker(name = "auth-service")
    override suspend fun signIn(command: SignInCommand.In): SignInCommand.Out {
        val signInResponse = authServiceStub.signIn(
            SignInRequest.newBuilder()
                .setEmail(command.email)
                .setPlainPassword(command.plainPassword)
                .build()
        )

        if (signInResponse.user.role != adminSecurityProperties.requiredRole) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required")
        }

        return SignInCommand.Out(
            user = UserDto(
                id = signInResponse.user.id,
                role = signInResponse.user.role,
                email = signInResponse.user.email,
                name = signInResponse.user.name,
            ),
            token = JwtDto(
                accessToken = signInResponse.token.accessToken,
                refreshToken = signInResponse.token.refreshToken,
            )
        )
    }
}
