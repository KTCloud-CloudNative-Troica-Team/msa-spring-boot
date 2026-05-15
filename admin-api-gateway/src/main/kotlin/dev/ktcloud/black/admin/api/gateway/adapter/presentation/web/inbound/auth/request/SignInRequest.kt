package dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.auth.request

data class SignInRequest(
    val email: String,
    val password: String,
)
