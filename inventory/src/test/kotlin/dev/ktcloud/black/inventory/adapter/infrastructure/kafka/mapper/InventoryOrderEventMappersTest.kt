package dev.ktcloud.black.inventory.adapter.infrastructure.kafka.mapper

import dev.ktcloud.black.inventory.adapter.infrastructure.kafka.model.InventoryReserveRequestMessage
import dev.ktcloud.black.inventory.adapter.infrastructure.kafka.model.InventoryReservedResultMessage
import dev.ktcloud.black.inventory.application.dto.event.inbound.InventoryReserveRequestEvent
import dev.ktcloud.black.inventory.application.dto.event.outbound.InventoryReservedResultEvent
import dev.ktcloud.black.inventory.domain.vo.InventoryReserveResultState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InventoryOrderEventMappersTest {
    @Test
    fun `listener mapper round-trips event and message`() {
        val mapper = InventoryOrderListenerEventMapper()
        val event = InventoryReserveRequestEvent(orderId = 1, inventoryId = 2, amount = 3)

        val message = mapper.toMessage(event)
        val back = mapper.toEvent(message)

        assertEquals(event, back)
        assertEquals(InventoryReserveRequestMessage(1, 2, 3), message)
    }

    @Test
    fun `publish mapper round-trips event and message`() {
        val mapper = InventoryOrderPublishEventMapper()
        val event = InventoryReservedResultEvent(orderId = 9, inventoryId = 8, amount = 1, resultState = InventoryReserveResultState.FAILED)

        val message = mapper.toMessage(event)
        val back = mapper.toEvent(message)

        assertEquals(event, back)
        assertEquals(InventoryReservedResultMessage(9, 8, 1, InventoryReserveResultState.FAILED), message)
    }
}
