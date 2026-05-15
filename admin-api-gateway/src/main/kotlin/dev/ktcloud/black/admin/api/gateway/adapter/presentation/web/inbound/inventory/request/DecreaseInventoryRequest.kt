package dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.request

data class DecreaseInventoryRequest(
    val orderId: Long,
    val amount: Int,
)
