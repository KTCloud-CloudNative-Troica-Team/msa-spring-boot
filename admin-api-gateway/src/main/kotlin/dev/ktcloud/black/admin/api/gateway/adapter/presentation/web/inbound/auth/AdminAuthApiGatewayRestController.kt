package dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.auth

import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.auth.request.SignInRequest
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.auth.response.CheckValidityResponse
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.auth.response.SignInResponse

interface AdminAuthApiGatewayRestController {
    suspend fun signIn(request: SignInRequest): SignInResponse
    suspend fun checkValidity(accessToken: String): CheckValidityResponse
}
