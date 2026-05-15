package dev.ktcloud.black.order.common.adapter.infrastructure.kafka

import dev.ktcloud.black.client.redis.api.IdempotentEventProcessor
import dev.ktcloud.black.order.order.application.dto.event.inbound.InventoryReservedResultEvent
import dev.ktcloud.black.order.order.application.service.OrderCommandService
import dev.ktcloud.black.order.order.domain.vo.InventoryReserveResultState
import dev.ktcloud.black.order.order.domain.vo.OrderLineItemStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test

class OrderInventoryEventKafkaListenerTest {
    private val service = mockk<OrderCommandService>(relaxed = true)
    private val idempotency = mockk<IdempotentEventProcessor>()
    private val sut = OrderInventoryEventKafkaListener(service, idempotency)

    @Test
    fun `onResultPublished marks line item INVENTORY_RESERVED on SUCCESS`() {
        val funcSlot = slot<() -> Unit>()
        every { idempotency.withIdempotencyProcess<Unit>(any(), any(), capture(funcSlot)) } answers {
            funcSlot.captured.invoke()
        }

        sut.onResultPublished(
            InventoryReservedResultEvent(orderId = 1, inventoryId = 2, amount = 3, resultState = InventoryReserveResultState.SUCCESS)
        )

        verify { service.updateOrderLineItemStatus(1L, 2L, OrderLineItemStatus.INVENTORY_RESERVED) }
    }

    @Test
    fun `onResultPublished marks line item FAILED on FAILED`() {
        val funcSlot = slot<() -> Unit>()
        every { idempotency.withIdempotencyProcess<Unit>(any(), any(), capture(funcSlot)) } answers {
            funcSlot.captured.invoke()
        }

        sut.onResultPublished(
            InventoryReservedResultEvent(orderId = 1, inventoryId = 2, amount = 3, resultState = InventoryReserveResultState.FAILED)
        )

        verify { service.updateOrderLineItemStatus(1L, 2L, OrderLineItemStatus.FAILED) }
    }
}
