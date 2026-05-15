package dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.request

data class CreateInventoryRequest(
    val productId: String,
    val skuCode: String,
)
