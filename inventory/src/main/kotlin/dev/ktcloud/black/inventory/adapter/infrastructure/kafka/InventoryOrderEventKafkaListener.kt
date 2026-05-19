package dev.ktcloud.black.inventory.adapter.infrastructure.kafka

import dev.ktcloud.black.inventory.adapter.infrastructure.kafka.mapper.InventoryOrderListenerEventMapper
import dev.ktcloud.black.inventory.adapter.infrastructure.kafka.model.InventoryReserveRequestMessage
import dev.ktcloud.black.inventory.application.port.event.InventoryOrderEventListenerPort
import dev.ktcloud.black.inventory.application.port.inbound.command.DecreaseInventoryCommand
import dev.ktcloud.black.inventory.application.service.InventoryCommandService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class InventoryOrderEventKafkaListener(
    private val inventoryCommandService: InventoryCommandService,
    private val mapper: InventoryOrderListenerEventMapper,
): InventoryOrderEventListenerPort {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${spring.kafka.topic.inventory-reserve-request}"],
        groupId = "inventory-service-group",
        containerFactory = "inventoryReserveRequestContainerFactory"
    )
    fun onReserveRequestMessage(record: ConsumerRecord<String, InventoryReserveRequestMessage>) {
        val value = record.value()
        log.info("onReserveRequest received: key={} value={} (type={})",
            record.key(), value, value?.javaClass?.name)
        if (value == null) {
            log.warn("skipping null value record offset={}", record.offset())
            return
        }
        onReserveRequest(mapper.toEvent(value))
    }

    override fun onReserveRequest(event: dev.ktcloud.black.inventory.application.dto.event.inbound.InventoryReserveRequestEvent) {
        inventoryCommandService.decrease(
            DecreaseInventoryCommand.In(
                orderId = event.orderId,
                inventoryId = event.inventoryId,
                amount = event.amount
            )
        )
    }
}
