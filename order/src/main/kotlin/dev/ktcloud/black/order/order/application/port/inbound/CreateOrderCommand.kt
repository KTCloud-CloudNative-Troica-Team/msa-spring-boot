package dev.ktcloud.black.order.order.application.port.inbound

import dev.ktcloud.black.order.order.application.dto.OrderLineItemDto
import dev.ktcloud.black.order.order.domain.vo.OrderStatus

interface CreateOrderCommand {
    fun create(command: List<In>): Out

    data class In(
        val inventoryId: Long,
        val productId: String,
        val skuCode: String,
        val price: Int,
        val quantity: Int,
    )

    data class Out(
        val id: Long,
        val status: OrderStatus,
        val orderLineItems: List<OrderLineItemDto>,
    )
}