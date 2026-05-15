package dev.ktcloud.black.order.order.application.service

import dev.ktcloud.black.order.order.application.port.inbound.FetchOrderQuery
import dev.ktcloud.black.order.order.application.port.outbound.OrderQueryOutboundPort
import dev.ktcloud.black.order.order.domain.entity.OrderDomainEntity
import dev.ktcloud.black.order.order.domain.vo.OrderLineItem
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrderQueryServiceTest {
    private val queryPort = mockk<OrderQueryOutboundPort>()
    private val sut = OrderQueryService(queryPort)

    @Test
    fun `fetchOrder maps order into Out`() {
        val order = OrderDomainEntity(
            id = 1,
            _orderLineItems = listOf(
                OrderLineItem(inventoryId = 9, productId = "p", skuCode = "s", price = 5, quantity = 2)
            )
        )
        every { queryPort.fetchOrder(1L) } returns order

        val out = sut.fetchOrder(FetchOrderQuery.In(1L))

        assertEquals(1L, out.id)
        assertEquals(1, out.orderLineItems.size)
        assertEquals(9L, out.orderLineItems.first().inventoryId)
    }

    @Test
    fun `fetchOrders returns mapped list of orders`() {
        every { queryPort.fetchAll() } returns listOf(
            OrderDomainEntity(id = 1, _orderLineItems = emptyList()),
            OrderDomainEntity(id = 2, _orderLineItems = emptyList())
        )

        val out = sut.fetchOrders()

        assertEquals(listOf(1L, 2L), out.map { it.id })
    }
}
