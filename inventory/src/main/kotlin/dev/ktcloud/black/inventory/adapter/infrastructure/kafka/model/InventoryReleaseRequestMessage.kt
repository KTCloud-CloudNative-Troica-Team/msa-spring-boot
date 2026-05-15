package dev.ktcloud.black.inventory.adapter.infrastructure.kafka.model

data class InventoryReleaseRequestMessage(
    val orderId: Long,
    val inventoryId: Long,
    val amount: Int
)
