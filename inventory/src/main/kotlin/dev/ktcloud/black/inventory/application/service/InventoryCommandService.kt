package dev.ktcloud.black.inventory.application.service

import dev.ktcloud.black.client.redis.api.DistributedLock
import dev.ktcloud.black.client.redis.api.IdempotentEventProcessor
import dev.ktcloud.black.inventory.application.dto.event.outbound.InventoryReservedResultEvent
import dev.ktcloud.black.inventory.application.port.event.InventoryOrderEventPublishPort
import dev.ktcloud.black.inventory.application.port.inbound.command.CreateInventoryCommand
import dev.ktcloud.black.inventory.application.port.inbound.command.DecreaseInventoryCommand
import dev.ktcloud.black.inventory.application.port.inbound.command.IncreaseInventoryCommand
import dev.ktcloud.black.inventory.application.port.outbound.LoadCacheSyncedInventoryOutboundPort
import dev.ktcloud.black.inventory.application.port.outbound.UpdateInventoryCommandOutboundPort
import dev.ktcloud.black.inventory.application.port.state.outbound.InventoryStateCommandOutboundPort
import dev.ktcloud.black.inventory.domain.entity.InventoryDomainEntity
import dev.ktcloud.black.inventory.domain.exception.InventoryException
import dev.ktcloud.black.inventory.domain.vo.InventoryDecreaseIdempotencyKey
import dev.ktcloud.black.inventory.domain.vo.InventoryLockKey
import dev.ktcloud.black.inventory.domain.vo.InventoryReserveResultState
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InventoryCommandService(
    private val inventoryStateCommandOutboundPort: InventoryStateCommandOutboundPort,
    private val loadCacheSyncedInventoryOutboundPort: LoadCacheSyncedInventoryOutboundPort,
    private val updateInventoryCommandOutboundPort: UpdateInventoryCommandOutboundPort,
    private val distributedLock: DistributedLock,
    private val idempotentEventProcessor: IdempotentEventProcessor,
    private val inventoryOrderEventPublishPort: InventoryOrderEventPublishPort
): CreateInventoryCommand, DecreaseInventoryCommand, IncreaseInventoryCommand {
    @Transactional
    override fun create(command: CreateInventoryCommand.In): CreateInventoryCommand.Out {
        val inventoryDomainEntity = InventoryDomainEntity(
            productId = command.productId,
            skuCode = command.skuCode,
        )

        val saved = inventoryStateCommandOutboundPort.save(inventoryDomainEntity)

        val loaded = distributedLock.execute(
            key = InventoryLockKey(saved.id).toLockKey(),
            func = {
                loadCacheSyncedInventoryOutboundPort.loadCacheSyncedInventory(saved.id)
            }
        )

        return CreateInventoryCommand.Out(
            id = loaded.id,
            productId = loaded.productId,
            skuCode = loaded.skuCode,
            quantity = loaded.quantity,
        )
    }

    @Transactional
    override fun decrease(command: DecreaseInventoryCommand.In): DecreaseInventoryCommand.Out {
        val inventory = try {
            loadCacheSyncedInventoryOutboundPort.loadInventory(command.inventoryId)
        } catch (_: InventoryException.NoCachedInventoryFound) {
            distributedLock.execute(
                key = InventoryLockKey(command.inventoryId).toLockKey(),
                func = {
                    loadCacheSyncedInventoryOutboundPort.loadCacheSyncedInventory(command.inventoryId)
                }
            )
        }

        val result = try {
            idempotentEventProcessor.withIdempotencyProcess(
                InventoryDecreaseIdempotencyKey(command.inventoryId, command.orderId).toIdempotencyKey(),
                func = {
                    updateInventoryCommandOutboundPort.decrease(command.inventoryId, command.amount)
                }
            )
        } catch (_: InventoryException.InventoryNotEnough) {
            inventoryOrderEventPublishPort.publish(
                InventoryReservedResultEvent(
                    orderId = command.inventoryId,
                    inventoryId = command.inventoryId,
                    amount = command.amount,
                    resultState = InventoryReserveResultState.FAILED
                )
            )

            null
        }

        if (result == null)
            throw InventoryException.InventoryNotEnough()

        inventoryOrderEventPublishPort.publish(
            InventoryReservedResultEvent(
                orderId = command.inventoryId,
                inventoryId = command.inventoryId,
                amount = command.amount,
                resultState = InventoryReserveResultState.SUCCESS
            )
        )

        inventory.setQuantity(result)

        return DecreaseInventoryCommand.Out(
            id = inventory.id,
            productId = inventory.productId,
            skuCode = inventory.skuCode,
            quantity = inventory.quantity,
        )
    }

    @Transactional
    override fun increase(command: IncreaseInventoryCommand.In): IncreaseInventoryCommand.Out {
        val inventory = try {
            loadCacheSyncedInventoryOutboundPort.loadInventory(command.inventoryId)
        } catch (_: InventoryException.NoCachedInventoryFound) {
            distributedLock.execute(
                key = InventoryLockKey(command.inventoryId).toLockKey(),
                func = {
                    loadCacheSyncedInventoryOutboundPort.loadCacheSyncedInventory(command.inventoryId)
                }
            )
        }

        val quantity = updateInventoryCommandOutboundPort.increase(command.inventoryId, command.amount)

        inventory.setQuantity(quantity)

        return IncreaseInventoryCommand.Out(
            id = inventory.id,
            productId = inventory.productId,
            skuCode = inventory.skuCode,
            quantity = inventory.quantity,
        )
    }
}