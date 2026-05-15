package dev.ktcloud.black.admin.api.gateway.application.product.port.inbound

interface CreateProductCommand {
    suspend fun createProduct(command: In): Out

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
