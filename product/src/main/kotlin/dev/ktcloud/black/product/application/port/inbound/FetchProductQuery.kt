package dev.ktcloud.black.product.application.port.inbound

interface FetchProductQuery {
    fun fetch(query: In): Out

    data class In(
        val id: String
    )

    data class Out(
        val id: String,
        val name: String,
        val description: String,
        val price: Int,
    )
}