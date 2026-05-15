package dev.ktcloud.black.admin.api.gateway.application.order.dto

data class OrderDto(
    val id: Long,
    val status: String,
    val orderLineItems: List<OrderLineItemDto>,
)
