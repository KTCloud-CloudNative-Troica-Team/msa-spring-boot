package dev.ktcloud.black.inventory.application.port.inbound.command

interface CreateInventoryCommand {
    fun create(command: In): Out

    data class In(
        val productId: String,
        val skuCode: String,
    )

    data class Out(
        val id: Long,
        val productId: String,
        val skuCode: String,
        val quantity: Int,
    )
}