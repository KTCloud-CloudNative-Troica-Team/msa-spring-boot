package dev.ktcloud.black.admin.api.gateway.application.inventory.service

import dev.ktcloud.black.admin.api.gateway.application.inventory.port.inbound.CreateInventoryCommand
import dev.ktcloud.black.admin.api.gateway.application.inventory.port.inbound.DecreaseInventoryCommand
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.InventoryResponseDto
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.InventoryServiceGrpcKt
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InventoryCommandServiceTest {
    private val stub = mockk<InventoryServiceGrpcKt.InventoryServiceCoroutineStub>()
    private val sut = InventoryCommandService(stub)

    @Test
    fun `createInventory delegates to stub and returns mapped Out`() = runBlocking {
        coEvery { stub.createInventory(any(), any()) } returns InventoryResponseDto.newBuilder()
            .setId(7).setProductId("p").setSkuCode("s").setQuantity(0).build()

        val out = sut.createInventory(CreateInventoryCommand.In(productId = "p", skuCode = "s"))

        assertEquals(7L, out.id)
    }

    @Test
    fun `decreaseInventory delegates to stub`() = runBlocking {
        coEvery { stub.decreaseInventory(any(), any()) } returns InventoryResponseDto.newBuilder()
            .setId(2).setProductId("p").setSkuCode("s").setQuantity(3).build()

        val out = sut.decreaseInventory(DecreaseInventoryCommand.In(orderId = 1, inventoryId = 2, amount = 1))

        assertEquals(3, out.quantity)
    }
}
