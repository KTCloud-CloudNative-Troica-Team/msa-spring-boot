package dev.ktcloud.black.user.adapter.security

import dev.ktcloud.black.user.domain.vo.UserDetail
import dev.ktcloud.black.user.domain.vo.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class SpringSecurityUserDetailsAdapterTest {
    @Test
    fun `authorities reflect user role`() {
        val sut = SpringSecurityUserDetailsAdapter(
            UserDetail(id = UUID.randomUUID(), role = UserRole.ADMIN, email = "e@x", name = "n")
        )

        val authorities = sut.authorities

        assertEquals(1, authorities.size)
        assertEquals("ADMIN", authorities.first().authority)
    }

    @Test
    fun `username is the user email and account flags are all true`() {
        val sut = SpringSecurityUserDetailsAdapter(
            UserDetail(id = UUID.randomUUID(), role = UserRole.USER, email = "u@x", name = "n")
        )

        assertEquals("u@x", sut.username)
        assertTrue(sut.isAccountNonExpired)
        assertTrue(sut.isAccountNonLocked)
        assertTrue(sut.isCredentialsNonExpired)
        assertTrue(sut.isEnabled)
    }
}
