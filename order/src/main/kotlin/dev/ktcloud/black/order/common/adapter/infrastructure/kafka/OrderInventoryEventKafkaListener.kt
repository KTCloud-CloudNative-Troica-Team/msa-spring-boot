package dev.ktcloud.black.order.common.adapter.infrastructure.kafka


import dev.ktcloud.black.client.redis.api.IdempotentEventProcessor
import dev.ktcloud.black.order.order.application.dto.event.inbound.InventoryReservedResultEvent
import dev.ktcloud.black.order.common.application.port.event.OrderInventoryEventListenerPort
import dev.ktcloud.black.order.order.application.service.OrderCommandService
import dev.ktcloud.black.order.order.domain.vo.InventoryReserveResultState
import dev.ktcloud.black.order.order.domain.vo.OrderInventoryResultIdempotencyKey
import dev.ktcloud.black.order.order.domain.vo.OrderLineItemStatus
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class OrderInventoryEventKafkaListener(
    private val orderCommandService: OrderCommandService,
    private val idempotentEventProcessor: IdempotentEventProcessor,
) : OrderInventoryEventListenerPort {
    @KafkaListener(
        topics = ["\${spring.kafka.topic.inventory-reserved-result}"],
        groupId = "inventory-service-group",
        containerFactory = "inventoryReservedResultContainerFactory"
    )
    override fun onResultPublished(event: InventoryReservedResultEvent) {
        idempotentEventProcessor.withIdempotencyProcess(
            key = OrderInventoryResultIdempotencyKey(
                orderId = event.orderId,
                inventoryId = event.inventoryId,
                resultState = event.resultState,
            ).toIdempotencyKey(),
            func = {
                val newStatus = when (event.resultState) {
                    InventoryReserveResultState.SUCCESS -> OrderLineItemStatus.INVENTORY_RESERVED
                    InventoryReserveResultState.FAILED -> OrderLineItemStatus.FAILED
                }

                orderCommandService.updateOrderLineItemStatus(
                    orderId = event.orderId,
                    inventoryId = event.inventoryId,
                    status = newStatus,
                )
            },
        )
    }
}
