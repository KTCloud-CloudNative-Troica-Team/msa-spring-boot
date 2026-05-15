package dev.ktcloud.black.inventory.adapter.infrastructure.kafka

import dev.ktcloud.black.inventory.application.dto.event.inbound.InventoryReserveRequestEvent
import dev.ktcloud.black.inventory.application.port.inbound.command.DecreaseInventoryCommand
import dev.ktcloud.black.inventory.application.service.InventoryCommandService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InventoryOrderEventKafkaListenerTest {
    private val service = mockk<InventoryCommandService>()
    private val sut = InventoryOrderEventKafkaListener(service)

    @Test
    fun `onReserveRequest forwards event to decrease command`() {
        val captured = slot<DecreaseInventoryCommand.In>()
        every { service.decrease(capture(captured)) } returns mockk()

        sut.onReserveRequest(InventoryReserveRequestEvent(orderId = 1, inventoryId = 2, amount = 3))

        assertEquals(1L, captured.captured.orderId)
        assertEquals(2L, captured.captured.inventoryId)
        assertEquals(3, captured.captured.amount)
    }
}
