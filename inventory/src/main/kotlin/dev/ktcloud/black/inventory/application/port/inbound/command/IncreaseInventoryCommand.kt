package dev.ktcloud.black.inventory.application.port.inbound.command

interface IncreaseInventoryCommand {
    fun increase(command: In): Out

    data class In(
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