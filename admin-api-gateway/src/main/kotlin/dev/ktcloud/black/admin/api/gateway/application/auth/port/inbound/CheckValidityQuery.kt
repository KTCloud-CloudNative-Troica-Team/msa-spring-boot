package dev.ktcloud.black.admin.api.gateway.application.auth.port.inbound

import dev.ktcloud.black.admin.api.gateway.application.auth.dto.UserDto

interface CheckValidityQuery {
    suspend fun checkValidity(query: In): Out

    data class In(
        val accessToken: String,
    )

    data class Out(
        val user: UserDto
    )
}
