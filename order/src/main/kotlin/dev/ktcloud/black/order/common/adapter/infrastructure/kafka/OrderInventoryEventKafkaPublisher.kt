package dev.ktcloud.black.order.common.adapter.infrastructure.kafka

import dev.ktcloud.black.order.common.adapter.infrastructure.kafka.mapper.OrderInventoryPublishEventMapper
import dev.ktcloud.black.order.common.adapter.infrastructure.kafka.model.InventoryReleaseRequestMessage
import dev.ktcloud.black.order.common.adapter.infrastructure.kafka.model.InventoryReserveRequestMessage
import dev.ktcloud.black.order.order.application.dto.event.outbound.InventoryReleaseRequestEvent
import dev.ktcloud.black.order.order.application.dto.event.outbound.InventoryReserveRequestEvent
import dev.ktcloud.black.order.common.application.port.event.OrderInventoryEventPublishPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class OrderInventoryEventKafkaPublisher(
    private val kafkaTemplate: KafkaTemplate<String, InventoryReserveRequestMessage>,
    private val releaseRequestKafkaTemplate: KafkaTemplate<String, InventoryReleaseRequestMessage>,
    private val mapper: OrderInventoryPublishEventMapper,
    @Value("\${spring.kafka.topic.inventory-reserve-request}")
    private val topicName: String,
    @Value("\${spring.kafka.topic.inventory-release-request}")
    private val releaseTopicName: String,
): OrderInventoryEventPublishPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(
        event: InventoryReserveRequestEvent,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val message = mapper.toMessage(event)

        log.info("kafka publish RESERVE topic={} orderId={} inventoryId={} amount={}",
            topicName, event.orderId, event.inventoryId, event.amount)
        try {
            kafkaTemplate.send(topicName, message.orderId.toString(), message).get()
            log.info("kafka publish RESERVE OK orderId={}", event.orderId)
            onSuccess.invoke()
        } catch (e: Exception) {
            log.error("kafka publish RESERVE FAILED orderId={}", event.orderId, e)
            onError.invoke()
        }
    }

    override fun publish(
        event: InventoryReleaseRequestEvent,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val message = mapper.toMessage(event)

        log.info("kafka publish RELEASE topic={} orderId={} inventoryId={} amount={}",
            releaseTopicName, event.orderId, event.inventoryId, event.amount)
        try {
            releaseRequestKafkaTemplate.send(releaseTopicName, message.orderId.toString(), message).get()
            log.info("kafka publish RELEASE OK orderId={}", event.orderId)
            onSuccess.invoke()
        } catch (e: Exception) {
            log.error("kafka publish RELEASE FAILED orderId={}", event.orderId, e)
            onError.invoke()
        }
    }
}
