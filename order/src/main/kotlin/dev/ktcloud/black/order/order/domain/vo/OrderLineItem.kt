package dev.ktcloud.black.order.order.domain.vo

data class OrderLineItem(
    val inventoryId: Long,
    val productId: String,
    val skuCode: String,
    val price: Int,
    val quantity: Int,
    val status: OrderLineItemStatus = OrderLineItemStatus.PENDING,
) {
    fun copy(newStatus: OrderLineItemStatus): OrderLineItem = copy(status = newStatus)
}
