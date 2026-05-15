package dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.order.response

import dev.ktcloud.black.admin.api.gateway.application.order.dto.OrderDto

data class FetchOrderResponse(
    val order: OrderDto
)
