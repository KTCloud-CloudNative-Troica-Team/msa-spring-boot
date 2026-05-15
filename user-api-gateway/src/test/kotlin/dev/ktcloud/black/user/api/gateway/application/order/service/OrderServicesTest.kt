package dev.ktcloud.black.user.api.gateway.application.order.service

import dev.ktcloud.black.order.service.adapter.presentation.web.inbound.grpc.OrderListItemResponseDto
import dev.ktcloud.black.order.service.adapter.presentation.web.inbound.grpc.OrderResponseDto
import dev.ktcloud.black.order.service.adapter.presentation.web.inbound.grpc.OrderServiceGrpcKt
import dev.ktcloud.black.user.api.gateway.application.order.port.inbound.CreateOrderCommand
import dev.ktcloud.black.user.api.gateway.application.order.port.inbound.FetchOrderQuery
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrderServicesTest {
    private val stub = mockk<OrderServiceGrpcKt.OrderServiceCoroutineStub>()

    @Test
    fun `OrderCommandService createOrder maps items into proto request`() = runBlocking {
        val service = OrderCommandService(stub)
        coEvery { stub.createOrder(any(), any()) } returns OrderResponseDto.newBuilder()
            .setId(7).setStatus("PENDING")
            .addOrderLineItems(
                OrderListItemResponseDto.newBuilder()
                    .setInventoryId(1).setProductId("p").setSkuCode("s").setPrice(10).setQuantity(2).setStatus("PENDING")
            )
            .build()

        val out = service.createOrder(
            listOf(
                CreateOrderCommand.In(inventoryId = 1, productId = "p", skuCode = "s", price = 10, quantity = 2)
            )
        )

        assertEquals(7L, out.id)
        coVerify { stub.createOrder(match { it.itemsCount == 1 }, any()) }
    }

    @Test
    fun `OrderQueryService fetchOrder returns mapped response`() = runBlocking {
        val service = OrderQueryService(stub)
        coEvery { stub.fetchOrder(any(), any()) } returns OrderResponseDto.newBuilder()
            .setId(9).setStatus("PENDING").build()

        val out = service.fetchOrder(FetchOrderQuery.In(9L))

        assertEquals(9L, out.id)
    }
}
