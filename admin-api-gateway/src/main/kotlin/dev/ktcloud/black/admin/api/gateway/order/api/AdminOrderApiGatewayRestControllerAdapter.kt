package dev.ktcloud.black.admin.api.gateway.order.api

import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.order.AdminOrderApiGatewayRestController
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.order.response.FetchOrderResponse
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.order.response.FetchOrdersResponse
import dev.ktcloud.black.admin.api.gateway.application.order.dto.OrderDto
import dev.ktcloud.black.admin.api.gateway.application.order.port.inbound.FetchOrderQuery
import dev.ktcloud.black.admin.api.gateway.application.order.port.inbound.FetchOrdersQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/api/v1/orders")
class AdminOrderApiGatewayRestControllerAdapter(
    private val fetchOrderQuery: FetchOrderQuery,
    private val fetchOrdersQuery: FetchOrdersQuery,
) : AdminOrderApiGatewayRestController {
    @Operation(summary = "주문 전체 조회 (관리자)")
    @ApiResponse(responseCode = "200", description = "주문 전체 조회 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping
    override suspend fun fetchOrders(): FetchOrdersResponse {
        val result = fetchOrdersQuery.fetchOrders()

        return FetchOrdersResponse(
            orders = result.map { OrderDto(it.id, it.status, it.orderLineItems) }
        )
    }

    @Operation(summary = "주문 단건 조회 (관리자)")
    @ApiResponse(responseCode = "200", description = "주문 조회 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping("{id}")
    override suspend fun fetchOrder(@PathVariable id: Long): FetchOrderResponse {
        val result = fetchOrderQuery.fetchOrder(
            FetchOrderQuery.In(id)
        )

        return FetchOrderResponse(
            order = OrderDto(
                id = result.id,
                status = result.status,
                orderLineItems = result.orderLineItems
            )
        )
    }
}
