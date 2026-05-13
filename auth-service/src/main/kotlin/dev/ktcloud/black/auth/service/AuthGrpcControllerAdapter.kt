package dev.ktcloud.black.auth.service

import dev.ktcloud.black.auth.application.port.inbound.CheckValidityQuery
import dev.ktcloud.black.auth.application.port.inbound.SignInCommand
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.CheckValidityRequest
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.Empty
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.SignInRequest
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.SignInResponse
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.SignUpRequest
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.TokenResponseDto
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.UserResponseDto
import dev.ktcloud.black.auth.service.adapter.web.inbound.AuthGrpcController
import dev.ktcloud.black.user.application.port.inbound.CreateUserCommand
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class AuthGrpcControllerAdapter(
    val createUserCommand: CreateUserCommand,
    val signInCommand: SignInCommand,
    val checkValidityQuery: CheckValidityQuery
): AuthGrpcController() {
    override suspend fun signUp(request: SignUpRequest): Empty {
        createUserCommand.create(
            CreateUserCommand.In(
                email = request.email,
                plainPassword = request.plainPassword,
                name = request.name,
            )
        )
        return Empty.getDefaultInstance()
    }

    override suspend fun signIn(request: SignInRequest): SignInResponse {
        val (token, user) = signInCommand.signIn(
            SignInCommand.In(
                email = request.email,
                password = request.plainPassword
            )
        )

        val tokenResponse = TokenResponseDto.newBuilder()
            .setAccessToken(token.accessToken)
            .setRefreshToken(token.refreshToken)
            .build()

        val userResponse = UserResponseDto.newBuilder()
            .setId(user.id.toString())
            .setEmail(user.email)
            .setRole(user.role.name)
            .setName(user.name)
            .build()

        return SignInResponse.newBuilder()
            .setToken(tokenResponse)
            .setUser(userResponse)
            .build()
    }

    override suspend fun checkValidity(request: CheckValidityRequest): UserResponseDto {
        val result = checkValidityQuery.checkValidity(
            CheckValidityQuery.In(accessToken = request.accessToken)
        )

        return UserResponseDto.newBuilder()
            .setId(result.id)
            .setEmail(result.email)
            .setRole(result.role)
            .setName(result.name)
            .build()
    }
}