package dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.configuration

import dev.ktcloud.black.admin.api.gateway.application.auth.dto.UserDto
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.AuthServiceGrpcKt
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.CheckValidityRequest
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtHeaderCheckFilter(
    @GrpcClient("auth-service")
    private val authServiceStub: AuthServiceGrpcKt.AuthServiceCoroutineStub,
    private val adminSecurityProperties: AdminSecurityProperties,
) : WebFilter {

    companion object {
        private const val ADMIN_PREFIX = "/admin/api/v1"
        private val PUBLIC_PATHS = listOf(
            "$ADMIN_PREFIX/auth/signin",
            "$ADMIN_PREFIX/auth/check",
        )

        fun isProtected(path: String): Boolean =
            path.startsWith(ADMIN_PREFIX) && PUBLIC_PATHS.none { path.startsWith(it) }
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (!isProtected(exchange.request.uri.path) || exchange.request.method == HttpMethod.OPTIONS)
            return chain.filter(exchange)

        return mono {
            val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token")
            }

            val token = authHeader.substring(7)

            val response = authServiceStub.checkValidity(
                CheckValidityRequest.newBuilder().setAccessToken(token).build()
            )

            if (response.role != adminSecurityProperties.requiredRole) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required")
            }

            val userDto = UserDto(
                id = response.id,
                role = response.role,
                email = response.email,
                name = response.name,
            )

            val authorities = listOf(SimpleGrantedAuthority(response.role))
            val auth = UsernamePasswordAuthenticationToken(userDto, null, authorities)

            chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .awaitSingleOrNull()
        }
    }
}
