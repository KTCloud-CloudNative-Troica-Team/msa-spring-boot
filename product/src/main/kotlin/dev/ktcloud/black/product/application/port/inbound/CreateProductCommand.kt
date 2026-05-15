package dev.ktcloud.black.product.application.port.inbound

interface CreateProductCommand {
    fun createProduct(command: In): Out

    data class In(
        val name: String,
        val description: String,
        val price: Int,
    )

    data class Out(
        val id: String,
        val name: String,
        val description: String,
        val price: Int,
    )
}