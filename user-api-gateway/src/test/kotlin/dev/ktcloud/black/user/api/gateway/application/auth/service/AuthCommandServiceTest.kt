package dev.ktcloud.black.user.api.gateway.application.auth.service

import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.AuthServiceGrpcKt
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.SignInResponse
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.TokenResponseDto
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.UserResponseDto
import dev.ktcloud.black.user.api.gateway.application.auth.port.inbound.SignInCommand
import dev.ktcloud.black.user.api.gateway.application.auth.port.inbound.SignUpCommand
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthCommandServiceTest {
    private val stub = mockk<AuthServiceGrpcKt.AuthServiceCoroutineStub>(relaxed = true)
    private val sut = AuthCommandService(stub)

    @Test
    fun `signUp delegates to gRPC stub with mapped fields`() = runBlocking {
        sut.signUp(SignUpCommand.In(email = "a@b", plainPassword = "pw", name = "n"))

        coVerify {
            stub.signUp(
                match {
                    it.email == "a@b" && it.plainPassword == "pw" && it.name == "n"
                },
                any()
            )
        }
    }

    @Test
    fun `signIn maps proto response into Out`() = runBlocking {
        val response = SignInResponse.newBuilder()
            .setUser(
                UserResponseDto.newBuilder()
                    .setId("id-1").setRole("USER").setEmail("a@b").setName("n")
            )
            .setToken(
                TokenResponseDto.newBuilder()
                    .setAccessToken("ACC").setRefreshToken("REF")
            )
            .build()
        coEvery { stub.signIn(any(), any()) } returns response

        val out = sut.signIn(SignInCommand.In(email = "a@b", plainPassword = "pw"))

        assertEquals("id-1", out.user.id)
        assertEquals("ACC", out.token.accessToken)
    }
}
