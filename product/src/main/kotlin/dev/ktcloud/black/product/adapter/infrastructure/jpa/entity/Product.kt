package dev.ktcloud.black.product.adapter.infrastructure.jpa.entity

import dev.ktcloud.black.common.adapter.infrastructure.jpa.BaseOrmEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SQLDelete
import org.hibernate.type.SqlTypes
import java.sql.Types
import java.util.UUID

@SQLDelete(sql = "UPDATE products SET deleted_at = NOW() WHERE id = ?")
@Entity
@Table(name = "products")
data class Product(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID,

    @Column
    val name: String,

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "TEXT")
    val description: String,

    @JdbcTypeCode(Types.NUMERIC)
    val price: Int,
): BaseOrmEntity()