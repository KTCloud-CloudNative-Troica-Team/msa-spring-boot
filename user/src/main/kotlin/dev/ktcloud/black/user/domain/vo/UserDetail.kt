package dev.ktcloud.black.user.domain.vo

import java.io.Serializable
import java.util.UUID

data class UserDetail(
    val id: UUID,
    val role: UserRole,
    val email: String,
    val name: String,
) : Serializable
