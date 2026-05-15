package dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.product.request

data class CreateProductRequest(
    val name: String,
    val description: String,
    val price: Int,
)
