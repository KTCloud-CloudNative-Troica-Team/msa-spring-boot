package dev.ktcloud.black.user.application.port.inbound

import dev.ktcloud.black.user.domain.vo.UserRole
import java.util.UUID

interface FetchAccountQuery {
    fun fetchAccount(query: In): Out

    data class In(
        val email: String,
        val plainPassword: String
    )

    data class Out(
        val id: UUID,
        val role: UserRole,
        val email: String,
        val name: String,
    )
}