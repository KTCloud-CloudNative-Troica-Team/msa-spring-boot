package dev.ktcloud.black.auth.application.service

import dev.ktcloud.black.auth.application.port.inbound.CheckValidityQuery
import dev.ktcloud.black.auth.application.service.jwt.JwtResolver
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthQueryService(
    private val jwtResolver: JwtResolver
): CheckValidityQuery {
    @Transactional(readOnly = true)
    override fun checkValidity(query: CheckValidityQuery.In): CheckValidityQuery.Out {
        val user = jwtResolver.validateToken(query.accessToken)

        return CheckValidityQuery.Out(
            id = user.id.toString(),
            role = user.role.toString(),
            email = user.email,
            name = user.name,
        )
    }
}