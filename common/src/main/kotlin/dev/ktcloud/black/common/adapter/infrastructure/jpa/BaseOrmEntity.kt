package dev.ktcloud.black.common.adapter.infrastructure.jpa

import dev.ktcloud.black.common.domain.entity.BaseEntity
import dev.ktcloud.black.common.util.time.now
import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class BaseOrmEntity(
    @CreatedDate
    @Column(nullable = false, updatable = false)
    override var createdAt: LocalDateTime = now(),

    @LastModifiedDate
    @Column(nullable = false, updatable = true)
    override var updatedAt: LocalDateTime = now(),

    override var deletedAt: LocalDateTime? = null,
) : BaseEntity {
    fun delete(time: LocalDateTime = now()) {
        deletedAt = time
    }
}
