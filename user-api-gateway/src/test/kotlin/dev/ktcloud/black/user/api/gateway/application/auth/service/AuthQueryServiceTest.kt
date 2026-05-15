package dev.ktcloud.black.user.api.gateway.application.auth.service

import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.AuthServiceGrpcKt
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.UserResponseDto
import dev.ktcloud.black.user.api.gateway.application.auth.port.inbound.CheckValidityQuery
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthQueryServiceTest {
    private val stub = mockk<AuthServiceGrpcKt.AuthServiceCoroutineStub>()
    private val sut = AuthQueryService(stub)

    @Test
    fun `checkValidity wraps gRPC response into Out`() = runBlocking {
        coEvery { stub.checkValidity(any(), any()) } returns UserResponseDto.newBuilder()
            .setId("id-1").setRole("USER").setEmail("u@x").setName("n").build()

        val out = sut.checkValidity(CheckValidityQuery.In(accessToken = "TOK"))

        assertEquals("id-1", out.user.id)
        assertEquals("USER", out.user.role)
    }
}
