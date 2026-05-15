package dev.ktcloud.black.order.order.application.service

import dev.ktcloud.black.order.common.application.port.event.OrderInventoryEventPublishPort
import dev.ktcloud.black.order.order.application.dto.OrderLineItemDto
import dev.ktcloud.black.order.order.application.dto.event.outbound.InventoryReleaseRequestEvent
import dev.ktcloud.black.order.order.application.port.inbound.CreateOrderCommand
import dev.ktcloud.black.order.order.application.port.outbound.OrderCommandOutboundPort
import dev.ktcloud.black.order.order.application.port.outbound.OrderQueryOutboundPort
import dev.ktcloud.black.order.order.domain.entity.OrderDomainEntity
import dev.ktcloud.black.order.order.domain.vo.OrderLineItem
import dev.ktcloud.black.order.order.domain.vo.OrderLineItemStatus
import dev.ktcloud.black.order.outbox.inventory.request.application.port.inbound.CreateOrderInventoryRequestOutboxCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderCommandService(
    private val orderCommandOutboundPort: OrderCommandOutboundPort,
    private val orderQueryOutboundPort: OrderQueryOutboundPort,
    private val createOrderInventoryRequestOutboxCommand: CreateOrderInventoryRequestOutboxCommand,
    private val orderInventoryEventPublishPort: OrderInventoryEventPublishPort,
): CreateOrderCommand {
    @Transactional
    override fun create(command: List<CreateOrderCommand.In>): CreateOrderCommand.Out {
        val orderDomainEntity = OrderDomainEntity(
            _orderLineItems = command.map {
                OrderLineItem(
                    inventoryId = it.inventoryId,
                    productId = it.productId,
                    skuCode = it.skuCode,
                    price = it.price,
                    quantity = it.quantity,
                )
            }
        )

        val savedOrder = orderCommandOutboundPort.save(orderDomainEntity)

        orderDomainEntity.orderLineItems.forEach {
            createOrderInventoryRequestOutboxCommand.create(
                CreateOrderInventoryRequestOutboxCommand.In(
                    orderId = savedOrder.id,
                    inventoryId = it.inventoryId,
                    amount = it.quantity
                )
            )
        }

        return CreateOrderCommand.Out(
            id = savedOrder.id,
            status = savedOrder.status,
            orderLineItems = savedOrder.orderLineItems.map { OrderLineItemDto.from(it) }
        )
    }

    @Transactional
    fun updateOrderLineItemStatus(orderId: Long, inventoryId: Long, status: OrderLineItemStatus) {
        val order = orderQueryOutboundPort.fetchOrder(orderId)

        order.updateOrderLineItem(inventoryId, status)

        if (status == OrderLineItemStatus.FAILED) {
            val peersToRelease = order.orderLineItems.filter {
                it.inventoryId != inventoryId && it.status == OrderLineItemStatus.INVENTORY_RESERVED
            }

            peersToRelease.forEach { peer ->
                orderInventoryEventPublishPort.publish(
                    InventoryReleaseRequestEvent(
                        orderId = order.id,
                        inventoryId = peer.inventoryId,
                        amount = peer.quantity,
                    )
                )
                order.markLineItemReleased(peer.inventoryId)
            }
        }

        orderCommandOutboundPort.save(order)
    }
}
