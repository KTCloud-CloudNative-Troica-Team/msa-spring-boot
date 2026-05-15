package dev.ktcloud.black.inventory.adapter.infrastructure.kafka

import dev.ktcloud.black.inventory.adapter.infrastructure.kafka.mapper.InventoryOrderPublishEventMapper
import dev.ktcloud.black.inventory.adapter.infrastructure.kafka.model.InventoryReservedResultMessage
import dev.ktcloud.black.inventory.application.dto.event.outbound.InventoryReservedResultEvent
import dev.ktcloud.black.inventory.domain.vo.InventoryReserveResultState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate

class InventoryOrderEventKafkaPublisherTest {
    private val template = mockk<KafkaTemplate<String, InventoryReservedResultMessage>>(relaxed = true)
    private val mapper = mockk<InventoryOrderPublishEventMapper>()
    private val topic = "inventory-reserved-result-topic"
    private val sut = InventoryOrderEventKafkaPublisher(template, mapper, topic)

    @Test
    fun `publish maps event and sends to configured topic with order id key`() {
        val event = InventoryReservedResultEvent(orderId = 7, inventoryId = 8, amount = 1, resultState = InventoryReserveResultState.SUCCESS)
        val message = InventoryReservedResultMessage(7, 8, 1, InventoryReserveResultState.SUCCESS)
        every { mapper.toMessage(event) } returns message

        sut.publish(event)

        verify { template.send(topic, "7", message) }
    }
}
