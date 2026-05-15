package dev.ktcloud.black.admin.api.gateway.application.order.port.inbound

import dev.ktcloud.black.admin.api.gateway.application.order.dto.OrderLineItemDto

interface FetchOrderQuery {
    suspend fun fetchOrder(query: In): Out

    data class In(
        val id: Long
    )

    data class Out(
        val id: Long,
        val status: String,
        val orderLineItems: List<OrderLineItemDto>,
    )
}
