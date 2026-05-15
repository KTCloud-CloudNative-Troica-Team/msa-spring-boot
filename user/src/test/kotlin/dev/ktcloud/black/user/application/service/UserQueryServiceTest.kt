package dev.ktcloud.black.user.application.service

import dev.ktcloud.black.user.adapter.infrastructure.jpa.repository.UserPostgresqlQueryRepository
import dev.ktcloud.black.user.application.port.inbound.FetchAccountQuery
import dev.ktcloud.black.user.application.port.inbound.FetchMeQuery
import dev.ktcloud.black.user.domain.entity.UserDomainEntity
import dev.ktcloud.black.user.domain.exception.UserException
import dev.ktcloud.black.user.domain.vo.UserRole
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

class UserQueryServiceTest {
    private val repo = mockk<UserPostgresqlQueryRepository>()
    private val encoder = mockk<PasswordEncoder>()
    private val sut = UserQueryService(repo, encoder)

    private fun user(email: String = "a@b.c") = UserDomainEntity(
        id = UUID.randomUUID(), role = UserRole.USER, email = email, password = "ENC", name = "n"
    )

    @Test
    fun `fetchMe returns Out for the given id`() {
        val u = user()
        every { repo.findById(u.id) } returns u

        val out = sut.fetchMe(FetchMeQuery.In(u.id))

        assertEquals(u.id, out.id)
        assertEquals(u.email, out.email)
    }

    @Test
    fun `fetchAccount returns Out when password matches`() {
        val u = user("k@e.c")
        every { repo.findByEmail("k@e.c") } returns u
        every { encoder.matches("plain", "ENC") } returns true

        val out = sut.fetchAccount(FetchAccountQuery.In(email = "k@e.c", plainPassword = "plain"))

        assertEquals(u.id, out.id)
    }

    @Test
    fun `fetchAccount throws UserNotFoundException when password mismatches`() {
        val u = user("k@e.c")
        every { repo.findByEmail("k@e.c") } returns u
        every { encoder.matches(any(), any()) } returns false

        assertThrows(UserException.UserNotFoundException::class.java) {
            sut.fetchAccount(FetchAccountQuery.In(email = "k@e.c", plainPassword = "wrong"))
        }
    }
}
