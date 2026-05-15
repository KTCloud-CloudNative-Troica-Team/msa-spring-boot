package dev.ktcloud.black.user.application.port.inbound

import dev.ktcloud.black.user.domain.vo.UserRole
import java.util.UUID

interface FetchMeQuery {
    fun fetchMe(query: In): Out

    data class In(val id: UUID)

    data class Out(
        val id: UUID,
        val role: UserRole,
        val email: String,
        val name: String,
    )
}