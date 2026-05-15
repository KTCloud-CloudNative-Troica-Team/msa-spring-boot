package dev.ktcloud.black.auth.application.service

import dev.ktcloud.black.auth.application.port.inbound.CheckValidityQuery
import dev.ktcloud.black.auth.application.service.jwt.JwtResolver
import dev.ktcloud.black.user.domain.vo.UserDetail
import dev.ktcloud.black.user.domain.vo.UserRole
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class AuthQueryServiceTest {
    private val resolver = mockk<JwtResolver>()
    private val sut = AuthQueryService(resolver)

    @Test
    fun `checkValidity flattens UserDetail into Out`() {
        val id = UUID.randomUUID()
        every { resolver.validateToken("TOK") } returns UserDetail(
            id = id, role = UserRole.ADMIN, email = "a@b", name = "n"
        )

        val out = sut.checkValidity(CheckValidityQuery.In(accessToken = "TOK"))

        assertEquals(id.toString(), out.id)
        assertEquals("ADMIN", out.role)
        assertEquals("a@b", out.email)
    }
}
