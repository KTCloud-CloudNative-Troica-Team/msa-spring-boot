package dev.ktcloud.black.order.order.adapter.infrastructure.jpa

import dev.ktcloud.black.order.order.adapter.infrastructure.jpa.embeddable.OrderLineItemJpaEmbeddable
import dev.ktcloud.black.order.order.adapter.infrastructure.jpa.entity.Order
import dev.ktcloud.black.order.order.domain.entity.OrderDomainEntity
import dev.ktcloud.black.order.order.domain.vo.OrderLineItem
import dev.ktcloud.black.order.order.domain.vo.OrderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrderMapperTest {
    private val sut = OrderMapper()

    @Test
    fun `toOrmEntity flattens line items into JPA embeddables`() {
        val domain = OrderDomainEntity(
            id = 5,
            _orderLineItems = listOf(
                OrderLineItem(inventoryId = 1, productId = "p", skuCode = "s", price = 1, quantity = 2)
            )
        )

        val orm = sut.toOrmEntity(domain)

        assertEquals(5, orm.id)
        assertEquals(1, orm.orderLineItems.size)
        assertEquals(2, orm.orderLineItems.first().quantity)
    }

    @Test
    fun `toDomainEntity rebuilds domain from JPA aggregate`() {
        val orm = Order(
            id = 6,
            status = OrderStatus.INVENTORY_RESERVED,
            orderLineItems = mutableListOf(
                OrderLineItemJpaEmbeddable(inventoryId = 1, productId = "p", skuCode = "s", price = 1, quantity = 1)
            )
        )

        val domain = sut.toDomainEntity(orm)

        assertEquals(OrderStatus.INVENTORY_RESERVED, domain.status)
        assertEquals(1, domain.orderLineItems.size)
    }
}
