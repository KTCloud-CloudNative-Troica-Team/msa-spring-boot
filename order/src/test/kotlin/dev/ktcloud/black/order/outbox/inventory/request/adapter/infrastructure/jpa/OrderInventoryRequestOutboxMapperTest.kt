package dev.ktcloud.black.order.outbox.inventory.request.adapter.infrastructure.jpa

import dev.ktcloud.black.order.outbox.inventory.request.adapter.infrastructure.jpa.entity.OrderInventoryRequestOutbox
import dev.ktcloud.black.order.outbox.inventory.request.domain.entity.OrderInventoryRequestOutboxDomainEntity
import dev.ktcloud.black.order.outbox.inventory.request.domain.vo.OrderInventoryRequestOutboxStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OrderInventoryRequestOutboxMapperTest {
    private val sut = OrderInventoryRequestOutboxMapper()

    @Test
    fun `toOrmEntity copies all fields`() {
        val time = LocalDateTime.now()
        val orm = sut.toOrmEntity(
            OrderInventoryRequestOutboxDomainEntity(
                id = 5,
                orderId = 1,
                inventoryId = 2,
                amount = 3,
                _retry = 2,
                _nextStartFrom = time
            )
        )

        assertEquals(5, orm.id)
        assertEquals(2, orm.retry)
        assertEquals(time, orm.nextStartFrom)
    }

    @Test
    fun `toDomainEntity copies all fields including non-default status`() {
        val orm = OrderInventoryRequestOutbox(
            id = 8,
            status = OrderInventoryRequestOutboxStatus.PUBLISHED,
            retry = 1,
            nextStartFrom = LocalDateTime.now(),
            orderId = 1,
            inventoryId = 2,
            amount = 4
        )

        val domain = sut.toDomainEntity(orm)

        assertEquals(OrderInventoryRequestOutboxStatus.PUBLISHED, domain.status)
        assertEquals(1, domain.retry)
    }
}
