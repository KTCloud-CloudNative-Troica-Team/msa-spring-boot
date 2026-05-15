package dev.ktcloud.black.order.order.domain.vo

data class OrderInventoryResultIdempotencyKey(
    val orderId: Long,
    val inventoryId: Long,
    val resultState: InventoryReserveResultState,
) {
    fun toIdempotencyKey() = "order-inventory-result:$orderId:$inventoryId:$resultState"

    override fun toString(): String = toIdempotencyKey()
}
