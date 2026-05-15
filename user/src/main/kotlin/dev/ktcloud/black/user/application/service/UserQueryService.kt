package dev.ktcloud.black.user.application.service

import dev.ktcloud.black.user.adapter.infrastructure.jpa.repository.UserPostgresqlQueryRepository
import dev.ktcloud.black.user.application.port.inbound.FetchAccountQuery
import dev.ktcloud.black.user.application.port.inbound.FetchMeQuery
import dev.ktcloud.black.user.domain.exception.UserException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserQueryService(
    private val userPostgresqlQueryRepository: UserPostgresqlQueryRepository,
    private val passwordEncoder: PasswordEncoder,
): FetchMeQuery, FetchAccountQuery {
    @Transactional(readOnly = true)
    override fun fetchMe(query: FetchMeQuery.In): FetchMeQuery.Out {
        val user = userPostgresqlQueryRepository.findById(query.id)

        return FetchMeQuery.Out(user.id, user.role, user.email, user.name)
    }

    @Transactional(readOnly = true)
    override fun fetchAccount(query: FetchAccountQuery.In): FetchAccountQuery.Out {
        val user = userPostgresqlQueryRepository.findByEmail(query.email)

        if (!passwordEncoder.matches(query.plainPassword, user.password))
            throw UserException.UserNotFoundException()

        return FetchAccountQuery.Out(user.id, user.role, user.email, user.name)
    }
}