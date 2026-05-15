package dev.ktcloud.black.inventory.adapter.infrastructure.kafka

import dev.ktcloud.black.client.redis.api.IdempotentEventProcessor
import dev.ktcloud.black.inventory.adapter.infrastructure.kafka.model.InventoryReleaseRequestMessage
import dev.ktcloud.black.inventory.application.port.inbound.command.IncreaseInventoryCommand
import dev.ktcloud.black.inventory.domain.vo.InventoryReleaseIdempotencyKey
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class InventoryReleaseEventKafkaListener(
    private val increaseInventoryCommand: IncreaseInventoryCommand,
    private val idempotentEventProcessor: IdempotentEventProcessor,
) {
    @KafkaListener(
        topics = ["\${spring.kafka.topic.inventory-release-request}"],
        groupId = "inventory-service-group",
        containerFactory = "inventoryReleaseRequestContainerFactory"
    )
    fun onReleaseRequest(message: InventoryReleaseRequestMessage) {
        idempotentEventProcessor.withIdempotencyProcess(
            key = InventoryReleaseIdempotencyKey(
                inventoryId = message.inventoryId,
                orderId = message.orderId,
            ).toIdempotencyKey(),
            func = {
                increaseInventoryCommand.increase(
                    IncreaseInventoryCommand.In(
                        inventoryId = message.inventoryId,
                        amount = message.amount,
                    )
                )
            }
        )
    }
}
