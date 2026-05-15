package dev.ktcloud.black.inventory.application.port.inbound.query

interface LoadInventoryQuery {
    fun load(query: In): Out

    data class In(
        val id: Long
    )

    data class Out(
        val id: Long,
        val productId: String,
        val skuCode: String,
        val quantity: Int
    )
}