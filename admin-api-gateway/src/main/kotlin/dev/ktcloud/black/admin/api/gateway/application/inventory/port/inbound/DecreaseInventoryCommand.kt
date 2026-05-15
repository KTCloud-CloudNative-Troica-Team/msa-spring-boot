package dev.ktcloud.black.admin.api.gateway.application.inventory.port.inbound

interface DecreaseInventoryCommand {
    suspend fun decreaseInventory(command: In): Out

    data class In(
        val orderId: Long,
        val inventoryId: Long,
        val amount: Int,
    )

    data class Out(
        val id: Long,
        val productId: String,
        val skuCode: String,
        val quantity: Int,
    )
}
