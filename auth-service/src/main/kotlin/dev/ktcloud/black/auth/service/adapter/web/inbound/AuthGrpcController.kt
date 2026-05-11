package dev.ktcloud.black.auth.service.adapter.web.inbound

import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.AuthServiceGrpcKt
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.CheckValidityRequest
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.Empty
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.SignInRequest
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.SignInResponse
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.SignUpRequest
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.UserResponseDto

abstract class AuthGrpcController: AuthServiceGrpcKt.AuthServiceCoroutineImplBase() {
    abstract override suspend fun signUp(request: SignUpRequest): Empty

    abstract override suspend fun signIn(request: SignInRequest): SignInResponse

    abstract override suspend fun checkValidity(request: CheckValidityRequest): UserResponseDto
}