package dev.ktcloud.black.admin.api.gateway.application.auth.service

import dev.ktcloud.black.admin.api.gateway.application.auth.dto.UserDto
import dev.ktcloud.black.admin.api.gateway.application.auth.port.inbound.CheckValidityQuery
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.AuthServiceGrpcKt
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.CheckValidityRequest
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service

@Service
class AuthQueryService(
    @GrpcClient("auth-service")
    private val authServiceStub: AuthServiceGrpcKt.AuthServiceCoroutineStub
) : CheckValidityQuery {
    @CircuitBreaker(name = "auth-service")
    override suspend fun checkValidity(query: CheckValidityQuery.In): CheckValidityQuery.Out {
        val checkResponse = authServiceStub.checkValidity(
            CheckValidityRequest.newBuilder()
                .setAccessToken(query.accessToken)
                .build()
        )

        return CheckValidityQuery.Out(
            user = UserDto(
                id = checkResponse.id,
                role = checkResponse.role,
                email = checkResponse.email,
                name = checkResponse.name,
            )
        )
    }
}
