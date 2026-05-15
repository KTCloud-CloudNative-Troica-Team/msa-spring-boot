package dev.ktcloud.black.admin.api.gateway.application.auth.service

import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.configuration.AdminSecurityProperties
import dev.ktcloud.black.admin.api.gateway.application.auth.port.inbound.SignInCommand
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.AuthServiceGrpcKt
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.SignInResponse
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.TokenResponseDto
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.UserResponseDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException

class AuthCommandServiceTest {
    private val stub = mockk<AuthServiceGrpcKt.AuthServiceCoroutineStub>()
    private val properties = AdminSecurityProperties(requiredRole = "ADMIN")
    private val sut = AuthCommandService(stub, properties)

    private fun signInResponse(role: String): SignInResponse = SignInResponse.newBuilder()
        .setUser(UserResponseDto.newBuilder().setId("id-1").setRole(role).setEmail("a@b").setName("n"))
        .setToken(TokenResponseDto.newBuilder().setAccessToken("ACC").setRefreshToken("REF"))
        .build()

    @Test
    fun `signIn returns Out when role matches required admin role`() = runBlocking {
        coEvery { stub.signIn(any(), any()) } returns signInResponse(role = "ADMIN")

        val out = sut.signIn(SignInCommand.In(email = "a@b", plainPassword = "pw"))

        assertEquals("ADMIN", out.user.role)
    }

    @Test
    fun `signIn throws FORBIDDEN when role does not match required admin role`(): Unit = runBlocking {
        coEvery { stub.signIn(any(), any()) } returns signInResponse(role = "USER")

        assertThrows(ResponseStatusException::class.java) {
            runBlocking {
                sut.signIn(SignInCommand.In(email = "a@b", plainPassword = "pw"))
            }
        }
    }
}
