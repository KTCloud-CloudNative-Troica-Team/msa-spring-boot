package dev.ktcloud.black.order.common.adapter.infrastructure.kafka.model

data class InventoryReleaseRequestMessage(
    val orderId: Long,
    val inventoryId: Long,
    val amount: Int
)
