package dev.ktcloud.black.inventory.adapter.infrastructure.jpa.entity

import dev.ktcloud.black.common.domain.entity.BaseOrmEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SQLDelete
import java.sql.Types

@SQLDelete(sql = "UPDATE inventories SET deleted_at = NOW() WHERE id = ?")
@Entity
@Table(name = "inventories")
data class Inventory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @Column("product_id")
    val productId: String,

    @Column("sku_code")
    val skuCode: String,

    @Column("quantity")
    val quantity: Int,
): BaseOrmEntity()