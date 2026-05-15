package dev.ktcloud.black.order.outbox.inventory.request.application.service

import dev.ktcloud.black.order.common.application.port.event.OrderInventoryEventPublishPort
import dev.ktcloud.black.order.order.application.dto.event.outbound.InventoryReserveRequestEvent
import dev.ktcloud.black.order.outbox.inventory.request.application.port.inbound.CreateOrderInventoryRequestOutboxCommand
import dev.ktcloud.black.order.outbox.inventory.request.application.port.outbound.OrderInventoryRequestCommandOutboundPort
import dev.ktcloud.black.order.outbox.inventory.request.application.port.outbound.OrderInventoryRequestQueryOutboundPort
import dev.ktcloud.black.order.outbox.inventory.request.domain.entity.OrderInventoryRequestOutboxDomainEntity
import dev.ktcloud.black.order.outbox.inventory.request.domain.vo.OrderInventoryRequestOutboxStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrderInventoryRequestOutboxServiceTest {
    private val commandPort = mockk<OrderInventoryRequestCommandOutboundPort>(relaxed = true)
    private val queryPort = mockk<OrderInventoryRequestQueryOutboundPort>()
    private val publisher = mockk<OrderInventoryEventPublishPort>()
    private val sut = OrderInventoryRequestOutboxCommandService(commandPort, queryPort, publisher)

    @Test
    fun `create persists a new outbox entry`() {
        val captured = slot<OrderInventoryRequestOutboxDomainEntity>()
        every { commandPort.save(capture(captured)) } returns Unit

        sut.create(CreateOrderInventoryRequestOutboxCommand.In(orderId = 1, inventoryId = 2, amount = 3))

        assertEquals(1L, captured.captured.orderId)
        assertEquals(OrderInventoryRequestOutboxStatus.INIT, captured.captured.status)
    }

    @Test
    fun `processAll publishes each unprocessed outbox event and marks PUBLISHED on success`() {
        val outbox = OrderInventoryRequestOutboxDomainEntity(orderId = 1, inventoryId = 2, amount = 3)
        every { queryPort.fetchUnprocessed() } returns listOf(outbox)

        val onSuccessSlot = slot<() -> Unit>()
        every {
            publisher.publish(any<InventoryReserveRequestEvent>(), capture(onSuccessSlot), any<() -> Unit>())
        } answers { onSuccessSlot.captured.invoke() }

        sut.processAll()

        assertEquals(OrderInventoryRequestOutboxStatus.PUBLISHED, outbox.status)
        assertEquals(1, outbox.retry)
        verify { publisher.publish(any<InventoryReserveRequestEvent>(), any<() -> Unit>(), any<() -> Unit>()) }
    }

    @Test
    fun `processAll marks FAILED after maximum retry exceeded`() {
        val outbox = OrderInventoryRequestOutboxDomainEntity(
            orderId = 1, inventoryId = 2, amount = 3, _retry = 5
        )
        every { queryPort.fetchUnprocessed() } returns listOf(outbox)

        sut.processAll()

        assertEquals(OrderInventoryRequestOutboxStatus.FAILED, outbox.status)
        verify(exactly = 0) { publisher.publish(any<InventoryReserveRequestEvent>(), any<() -> Unit>(), any<() -> Unit>()) }
    }
}
