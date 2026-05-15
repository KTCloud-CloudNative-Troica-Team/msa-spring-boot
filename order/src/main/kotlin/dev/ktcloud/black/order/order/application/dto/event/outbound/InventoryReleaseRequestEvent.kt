package dev.ktcloud.black.order.order.application.dto.event.outbound

data class InventoryReleaseRequestEvent(
    val orderId: Long,
    val inventoryId: Long,
    val amount: Int
)
