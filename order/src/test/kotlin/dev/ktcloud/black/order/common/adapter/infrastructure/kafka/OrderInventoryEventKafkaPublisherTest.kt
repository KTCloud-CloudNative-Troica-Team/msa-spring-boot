package dev.ktcloud.black.order.common.adapter.infrastructure.kafka

import dev.ktcloud.black.order.common.adapter.infrastructure.kafka.mapper.OrderInventoryPublishEventMapper
import dev.ktcloud.black.order.common.adapter.infrastructure.kafka.model.InventoryReleaseRequestMessage
import dev.ktcloud.black.order.common.adapter.infrastructure.kafka.model.InventoryReserveRequestMessage
import dev.ktcloud.black.order.order.application.dto.event.outbound.InventoryReserveRequestEvent
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

class OrderInventoryEventKafkaPublisherTest {
    private val template = mockk<KafkaTemplate<String, InventoryReserveRequestMessage>>()
    private val releaseTemplate = mockk<KafkaTemplate<String, InventoryReleaseRequestMessage>>(relaxed = true)
    private val mapper = mockk<OrderInventoryPublishEventMapper>()
    private val sut = OrderInventoryEventKafkaPublisher(template, releaseTemplate, mapper, "topic-x", "release-topic")

    @Test
    fun `publish invokes onSuccess when send completes successfully`() {
        val event = InventoryReserveRequestEvent(orderId = 1, inventoryId = 2, amount = 3)
        val message = InventoryReserveRequestMessage(1, 2, 3)
        every { mapper.toMessage(event) } returns message

        val future = CompletableFuture<SendResult<String, InventoryReserveRequestMessage>>()
        every { template.send("topic-x", "1", message) } returns future

        var success = false
        sut.publish(event, onSuccess = { success = true }, onError = { })

        future.complete(SendResult(mockk(relaxed = true), mockk<RecordMetadata>(relaxed = true)))
        assertTrue(success)
    }

    @Test
    fun `publish invokes onError when send fails`() {
        val event = InventoryReserveRequestEvent(orderId = 1, inventoryId = 2, amount = 3)
        val message = InventoryReserveRequestMessage(1, 2, 3)
        every { mapper.toMessage(event) } returns message
        val future = CompletableFuture<SendResult<String, InventoryReserveRequestMessage>>()
        every { template.send("topic-x", "1", message) } returns future

        var failed = false
        sut.publish(event, onSuccess = { }, onError = { failed = true })

        future.completeExceptionally(RuntimeException("boom"))
        assertTrue(failed)
    }
}
