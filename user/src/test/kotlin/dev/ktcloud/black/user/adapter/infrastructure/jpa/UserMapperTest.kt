package dev.ktcloud.black.user.adapter.infrastructure.jpa

import dev.ktcloud.black.user.adapter.infrastructure.jpa.entity.User
import dev.ktcloud.black.user.domain.entity.UserDomainEntity
import dev.ktcloud.black.user.domain.vo.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class UserMapperTest {
    private val sut = UserMapper()

    @Test
    fun `toOrmEntity copies all fields`() {
        val id = UUID.randomUUID()
        val orm = sut.toOrmEntity(
            UserDomainEntity(id = id, role = UserRole.ADMIN, email = "e@x", password = "p", name = "n")
        )
        assertEquals(id, orm.id)
        assertEquals(UserRole.ADMIN, orm.role)
        assertEquals("e@x", orm.email)
    }

    @Test
    fun `toDomainEntity copies all fields`() {
        val id = UUID.randomUUID()
        val domain = sut.toDomainEntity(
            User(id = id, role = UserRole.USER, email = "a@b", password = "p", name = "n")
        )
        assertEquals(id, domain.id)
        assertEquals(UserRole.USER, domain.role)
    }
}
