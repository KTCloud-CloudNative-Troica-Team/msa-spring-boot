package dev.ktcloud.black.order.order.application.port.inbound

import dev.ktcloud.black.order.order.application.dto.OrderLineItemDto
import dev.ktcloud.black.order.order.domain.vo.OrderStatus

interface FetchOrdersQuery {
    fun fetchOrders(): List<Out>

    data class Out(
        val id: Long,
        val status: OrderStatus,
        val orderLineItems: List<OrderLineItemDto>
    )
}