package dev.ktcloud.black.inventory.event.adapter.infrastructure.jpa.entity

import dev.ktcloud.black.common.adapter.infrastructure.jpa.BaseOrmEntity
import dev.ktcloud.black.inventory.event.domain.vo.InventoryEventProcessStatus
import dev.ktcloud.black.inventory.event.domain.vo.InventoryEventType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLDelete

@SQLDelete(sql = "UPDATE inventory_events SET deleted_at = NOW() WHERE id = ?")
@Entity
@Table(name = "inventory_events")
data class InventoryEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @Column(name = "inventory_id")
    val inventoryId: Long,

    @Column
    val amount: Int,

    @Column(name = "event_type")
    @Enumerated(EnumType.STRING)
    val eventType: InventoryEventType,

    @Column(name = "process_status")
    @Enumerated(EnumType.STRING)
    val processStatus: InventoryEventProcessStatus,
): BaseOrmEntity()