package dev.ktcloud.black.order.order.application.port.inbound

import dev.ktcloud.black.order.order.application.dto.OrderLineItemDto
import dev.ktcloud.black.order.order.domain.vo.OrderStatus

interface FetchOrderQuery {
    fun fetchOrder(query: In): Out

    data class In(
        val id: Long
    )

    data class Out(
        val id: Long,
        val status: OrderStatus,
        val orderLineItems: List<OrderLineItemDto>
    )
}