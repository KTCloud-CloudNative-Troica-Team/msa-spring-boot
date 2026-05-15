package dev.ktcloud.black.order.order.adapter.infrastructure.jpa.repository

import dev.ktcloud.black.order.order.adapter.infrastructure.jpa.OrderMapper
import dev.ktcloud.black.order.order.adapter.infrastructure.jpa.entity.Order
import dev.ktcloud.black.order.order.domain.entity.OrderDomainEntity
import dev.ktcloud.black.order.order.domain.exception.OrderException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Optional

class OrderPostgresqlRepositoryTest {
    private val repo = mockk<OrderPostgresqlRepository>()
    private val mapper = mockk<OrderMapper>()
    private val command = OrderPostgresqlCommandRepository(repo, mapper)
    private val query = OrderPostgresqlQueryRepository(repo, mapper)

    @Test
    fun `save round-trips through mapper`() {
        val domain = OrderDomainEntity(_orderLineItems = emptyList())
        val orm = mockk<Order>()
        every { mapper.toOrmEntity(domain) } returns orm
        every { repo.save(orm) } returns orm
        every { mapper.toDomainEntity(orm) } returns domain

        assertEquals(domain, command.save(domain))
    }

    @Test
    fun `fetchOrder throws NoSuchOrder when missing`() {
        every { repo.findById(99L) } returns Optional.empty()

        assertThrows(OrderException.NoSuchOrder::class.java) { query.fetchOrder(99L) }
    }
}
