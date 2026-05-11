package dev.ktcloud.black.user.adapter.infrastructure.jpa.entity

import dev.ktcloud.black.common.domain.entity.BaseOrmEntity
import dev.ktcloud.black.user.domain.vo.UserRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLDelete
import java.util.UUID

@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE id = ?")
@Entity
@Table(name = "users")
data class User(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID,

    @Column
    @Enumerated(EnumType.STRING)
    val role: UserRole,

    @Column(unique = true)
    val email: String,

    val password: String,

    val name: String
): BaseOrmEntity()