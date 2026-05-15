package dev.ktcloud.black.order.service

import dev.ktcloud.black.order.order.application.dto.OrderLineItemDto
import dev.ktcloud.black.order.order.application.port.inbound.CreateOrderCommand
import dev.ktcloud.black.order.order.application.port.inbound.FetchOrderQuery
import dev.ktcloud.black.order.order.application.port.inbound.FetchOrdersQuery
import dev.ktcloud.black.order.order.domain.vo.OrderLineItemStatus
import dev.ktcloud.black.order.order.domain.vo.OrderStatus
import dev.ktcloud.black.order.service.adapter.presentation.web.inbound.grpc.CreateOrderRequest
import dev.ktcloud.black.order.service.adapter.presentation.web.inbound.grpc.CreateOrderRequestItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrderGrpcControllerAdapterTest {
    private val create = mockk<CreateOrderCommand>()
    private val fetchOne = mockk<FetchOrderQuery>()
    private val fetchAll = mockk<FetchOrdersQuery>()
    private val sut = OrderGrpcControllerAdapter(create, fetchOne, fetchAll)

    @Test
    fun `createOrder maps proto items to commands and returns proto response`() = runBlocking {
        every { create.create(any()) } returns CreateOrderCommand.Out(
            id = 7,
            status = OrderStatus.PENDING,
            orderLineItems = listOf(
                OrderLineItemDto(inventoryId = 1, productId = "p", skuCode = "s", price = 10, quantity = 2, status = OrderLineItemStatus.PENDING)
            )
        )

        val request = CreateOrderRequest.newBuilder()
            .addItems(
                CreateOrderRequestItem.newBuilder()
                    .setInventoryId(1).setProductId("p").setSkuCode("s").setPrice(10).setQuantity(2).build()
            )
            .build()

        val response = sut.createOrder(request)

        assertEquals(7L, response.id)
        assertEquals("PENDING", response.status)
        assertEquals(1, response.orderLineItemsCount)
    }
}
