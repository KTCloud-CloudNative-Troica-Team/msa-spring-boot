package dev.ktcloud.black.order.order.application.service

import dev.ktcloud.black.order.common.application.port.event.OrderInventoryEventPublishPort
import dev.ktcloud.black.order.order.application.port.inbound.CreateOrderCommand
import dev.ktcloud.black.order.order.application.port.outbound.OrderCommandOutboundPort
import dev.ktcloud.black.order.order.application.port.outbound.OrderQueryOutboundPort
import dev.ktcloud.black.order.order.domain.entity.OrderDomainEntity
import dev.ktcloud.black.order.order.domain.vo.OrderLineItem
import dev.ktcloud.black.order.order.domain.vo.OrderLineItemStatus
import dev.ktcloud.black.order.order.domain.vo.OrderStatus
import dev.ktcloud.black.order.outbox.inventory.request.application.port.inbound.CreateOrderInventoryRequestOutboxCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrderCommandServiceTest {
    private val command = mockk<OrderCommandOutboundPort>()
    private val query = mockk<OrderQueryOutboundPort>()
    private val outbox = mockk<CreateOrderInventoryRequestOutboxCommand>(relaxed = true)
    private val inventoryEventPublishPort = mockk<OrderInventoryEventPublishPort>(relaxed = true)
    private val sut = OrderCommandService(command, query, outbox, inventoryEventPublishPort)

    @Test
    fun `create persists order and emits one outbox row per line item`() {
        val captured = slot<OrderDomainEntity>()
        every { command.save(capture(captured)) } answers {
            OrderDomainEntity(
                id = 42,
                _orderLineItems = captured.captured.orderLineItems
            )
        }

        val out = sut.create(
            listOf(
                CreateOrderCommand.In(inventoryId = 1, productId = "p1", skuCode = "s1", price = 10, quantity = 2),
                CreateOrderCommand.In(inventoryId = 2, productId = "p2", skuCode = "s2", price = 20, quantity = 1)
            )
        )

        assertEquals(42L, out.id)
        assertEquals(OrderStatus.PENDING, out.status)
        verify(exactly = 2) { outbox.create(any()) }
    }

    @Test
    fun `updateOrderLineItemStatus marks line item and persists`() {
        val order = OrderDomainEntity(
            id = 7,
            _orderLineItems = listOf(
                OrderLineItem(inventoryId = 1, productId = "p", skuCode = "s", price = 1, quantity = 1)
            )
        )
        every { query.fetchOrder(7L) } returns order
        every { command.save(order) } returns order

        sut.updateOrderLineItemStatus(7L, 1L, OrderLineItemStatus.INVENTORY_RESERVED)

        assertEquals(OrderStatus.INVENTORY_RESERVED, order.status)
        verify { command.save(order) }
    }
}
