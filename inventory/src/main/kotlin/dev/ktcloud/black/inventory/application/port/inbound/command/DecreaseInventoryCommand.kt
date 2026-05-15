package dev.ktcloud.black.inventory.application.port.inbound.command

interface DecreaseInventoryCommand {
    fun decrease(command: In): Out

    data class In(
        val orderId: Long,
        val inventoryId: Long,
        val amount: Int
    )

    data class Out(
        val id: Long,
        val productId: String,
        val skuCode: String,
        val quantity: Int,
    )
}