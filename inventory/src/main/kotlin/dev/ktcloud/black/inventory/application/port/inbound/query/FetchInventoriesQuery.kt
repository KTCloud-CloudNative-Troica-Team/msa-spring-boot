package dev.ktcloud.black.inventory.application.port.inbound.query

interface FetchInventoriesQuery {
    fun fetchAll(): List<Out>

    data class Out(
        val id: Long,
        val productId: String,
        val skuCode: String,
        val quantity: Int
    )
}