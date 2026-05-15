package dev.ktcloud.black.admin.api.gateway.application.inventory.service

import dev.ktcloud.black.admin.api.gateway.application.inventory.port.inbound.CreateInventoryCommand
import dev.ktcloud.black.admin.api.gateway.application.inventory.port.inbound.DecreaseInventoryCommand
import dev.ktcloud.black.admin.api.gateway.application.inventory.port.inbound.IncreaseInventoryCommand
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.CreateInventoryRequest
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.DecreaseInventoryRequest
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.IncreaseInventoryRequest
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.InventoryServiceGrpcKt
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service

@Service
class InventoryCommandService(
    @GrpcClient("inventory-service")
    private val inventoryServiceStub: InventoryServiceGrpcKt.InventoryServiceCoroutineStub
) : CreateInventoryCommand, IncreaseInventoryCommand, DecreaseInventoryCommand {
    override suspend fun createInventory(command: CreateInventoryCommand.In): CreateInventoryCommand.Out {
        val response = inventoryServiceStub.createInventory(
            CreateInventoryRequest.newBuilder()
                .setProductId(command.productId)
                .setSkuCode(command.skuCode)
                .build()
        )

        return CreateInventoryCommand.Out(
            id = response.id,
            productId = response.productId,
            skuCode = response.skuCode,
            quantity = response.quantity,
        )
    }

    override suspend fun increaseInventory(command: IncreaseInventoryCommand.In): IncreaseInventoryCommand.Out {
        val response = inventoryServiceStub.increaseInventory(
            IncreaseInventoryRequest.newBuilder()
                .setInventoryId(command.inventoryId)
                .setAmount(command.amount)
                .build()
        )

        return IncreaseInventoryCommand.Out(
            id = response.id,
            productId = response.productId,
            skuCode = response.skuCode,
            quantity = response.quantity,
        )
    }

    override suspend fun decreaseInventory(command: DecreaseInventoryCommand.In): DecreaseInventoryCommand.Out {
        val response = inventoryServiceStub.decreaseInventory(
            DecreaseInventoryRequest.newBuilder()
                .setOrderId(command.orderId)
                .setInventoryId(command.inventoryId)
                .setAmount(command.amount)
                .build()
        )

        return DecreaseInventoryCommand.Out(
            id = response.id,
            productId = response.productId,
            skuCode = response.skuCode,
            quantity = response.quantity,
        )
    }
}
