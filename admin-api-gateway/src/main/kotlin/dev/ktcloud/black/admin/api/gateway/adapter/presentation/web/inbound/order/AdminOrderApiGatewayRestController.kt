package dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.order

import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.order.response.FetchOrderResponse
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.order.response.FetchOrdersResponse

interface AdminOrderApiGatewayRestController {
    suspend fun fetchOrders(): FetchOrdersResponse
    suspend fun fetchOrder(id: Long): FetchOrderResponse
}
