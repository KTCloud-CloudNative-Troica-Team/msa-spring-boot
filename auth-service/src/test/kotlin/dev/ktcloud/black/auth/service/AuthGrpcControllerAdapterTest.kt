package dev.ktcloud.black.auth.service

import dev.ktcloud.black.auth.application.dto.JwtTokenDto
import dev.ktcloud.black.auth.application.port.inbound.CheckValidityQuery
import dev.ktcloud.black.auth.application.port.inbound.SignInCommand
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.CheckValidityRequest
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.SignInRequest
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.SignUpRequest
import dev.ktcloud.black.user.application.dto.UserDto
import dev.ktcloud.black.user.application.port.inbound.CreateUserCommand
import dev.ktcloud.black.user.domain.vo.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class AuthGrpcControllerAdapterTest {
    private val createUser = mockk<CreateUserCommand>(relaxed = true)
    private val signIn = mockk<SignInCommand>()
    private val checkValidity = mockk<CheckValidityQuery>()
    private val sut = AuthGrpcControllerAdapter(createUser, signIn, checkValidity)

    @Test
    fun `signUp delegates to CreateUserCommand and returns Empty`() = runBlocking {
        val captured = slot<CreateUserCommand.In>()
        every { createUser.create(capture(captured)) } returns mockk()

        sut.signUp(
            SignUpRequest.newBuilder().setEmail("a@b").setPlainPassword("pw").setName("n").build()
        )

        assertEquals("a@b", captured.captured.email)
        assertEquals("pw", captured.captured.plainPassword)
        verify { createUser.create(any()) }
    }

    @Test
    fun `signIn maps tokens and user into proto response`() = runBlocking {
        val id = UUID.randomUUID()
        every { signIn.signIn(any()) } returns SignInCommand.Out(
            token = JwtTokenDto(accessToken = "ACC", refreshToken = "REF"),
            user = UserDto(id = id, role = UserRole.USER, email = "a@b", name = "n")
        )

        val response = sut.signIn(
            SignInRequest.newBuilder().setEmail("a@b").setPlainPassword("pw").build()
        )

        assertEquals("ACC", response.token.accessToken)
        assertEquals(id.toString(), response.user.id)
        assertEquals("USER", response.user.role)
    }

    @Test
    fun `checkValidity returns user response from query result`() = runBlocking {
        every { checkValidity.checkValidity(any()) } returns CheckValidityQuery.Out(
            id = "id-1", role = "USER", email = "a@b", name = "n"
        )

        val response = sut.checkValidity(
            CheckValidityRequest.newBuilder().setAccessToken("TOK").build()
        )

        assertEquals("id-1", response.id)
        assertEquals("USER", response.role)
    }
}
