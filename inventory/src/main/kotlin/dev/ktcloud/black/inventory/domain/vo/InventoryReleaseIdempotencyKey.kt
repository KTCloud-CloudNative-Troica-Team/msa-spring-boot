package dev.ktcloud.black.inventory.domain.vo

data class InventoryReleaseIdempotencyKey(
    val inventoryId: Long,
    val orderId: Long
) {
    fun toIdempotencyKey() = "inventory-release:$inventoryId:$orderId"

    override fun toString(): String = toIdempotencyKey()
}
