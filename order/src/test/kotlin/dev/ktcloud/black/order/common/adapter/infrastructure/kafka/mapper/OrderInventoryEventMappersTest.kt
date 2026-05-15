package dev.ktcloud.black.order.common.adapter.infrastructure.kafka.mapper

import dev.ktcloud.black.order.common.adapter.infrastructure.kafka.model.InventoryReservedResultMessage
import dev.ktcloud.black.order.order.application.dto.event.inbound.InventoryReservedResultEvent
import dev.ktcloud.black.order.order.application.dto.event.outbound.InventoryReserveRequestEvent
import dev.ktcloud.black.order.order.domain.vo.InventoryReserveResultState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrderInventoryEventMappersTest {
    @Test
    fun `listen mapper round-trips event and message`() {
        val mapper = OrderInventoryListenEventMapper()
        val event = InventoryReservedResultEvent(orderId = 9, inventoryId = 8, amount = 1, resultState = InventoryReserveResultState.SUCCESS)

        assertEquals(event, mapper.toEvent(mapper.toMessage(event)))
        assertEquals(InventoryReservedResultMessage(9, 8, 1, InventoryReserveResultState.SUCCESS), mapper.toMessage(event))
    }

    @Test
    fun `publish mapper round-trips event and message`() {
        val mapper = OrderInventoryPublishEventMapper()
        val event = InventoryReserveRequestEvent(orderId = 1, inventoryId = 2, amount = 3)

        assertEquals(event, mapper.toEvent(mapper.toMessage(event)))
    }
}
