package dev.ktcloud.black.user.adapter.infrastructure.jpa.repository

import dev.ktcloud.black.user.adapter.infrastructure.jpa.UserMapper
import dev.ktcloud.black.user.adapter.infrastructure.jpa.entity.User
import dev.ktcloud.black.user.domain.entity.UserDomainEntity
import dev.ktcloud.black.user.domain.exception.UserException
import dev.ktcloud.black.user.domain.vo.UserRole
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class UserPostgresqlRepositoryTest {
    private val repo = mockk<UserPostgresqlRepository>()
    private val mapper = mockk<UserMapper>()
    private val command = UserPostgresqlCommandRepository(repo, mapper)
    private val query = UserPostgresqlQueryRepository(repo, mapper)

    @Test
    fun `save round-trips through mapper`() {
        val domain = UserDomainEntity(role = UserRole.USER, email = "a", password = "b", name = "c")
        val orm = mockk<User>()
        every { mapper.toOrmEntity(domain) } returns orm
        every { repo.save(orm) } returns orm
        every { mapper.toDomainEntity(orm) } returns domain

        assertEquals(domain, command.save(domain))
    }

    @Test
    fun `findById throws when missing`() {
        val id = UUID.randomUUID()
        every { repo.findById(id) } returns Optional.empty()

        assertThrows(UserException.UserNotFoundException::class.java) { query.findById(id) }
    }

    @Test
    fun `findByEmail throws when missing`() {
        every { repo.findByEmail("missing@x") } returns null

        assertThrows(UserException.UserNotFoundException::class.java) { query.findByEmail("missing@x") }
    }
}
