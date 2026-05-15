package dev.ktcloud.black.admin.api.gateway.application.inventory.port.inbound

interface CreateInventoryCommand {
    suspend fun createInventory(command: In): Out

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
