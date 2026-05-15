package dev.ktcloud.black.admin.api.gateway.application.auth.dto

data class JwtDto(
    val accessToken: String,
    val refreshToken: String,
)
