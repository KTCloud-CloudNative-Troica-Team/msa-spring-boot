package dev.ktcloud.black.inventory.service

import dev.ktcloud.black.inventory.application.port.inbound.command.CreateInventoryCommand
import dev.ktcloud.black.inventory.application.port.inbound.command.DecreaseInventoryCommand
import dev.ktcloud.black.inventory.application.port.inbound.command.IncreaseInventoryCommand
import dev.ktcloud.black.inventory.application.port.inbound.query.FetchInventoriesQuery
import dev.ktcloud.black.inventory.application.port.inbound.query.LoadInventoryQuery
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.CreateInventoryRequest
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.DecreaseInventoryRequest
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.Empty
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InventoryGrpcControllerAdapterTest {
    private val create = mockk<CreateInventoryCommand>()
    private val decrease = mockk<DecreaseInventoryCommand>()
    private val increase = mockk<IncreaseInventoryCommand>()
    private val fetchAll = mockk<FetchInventoriesQuery>()
    private val load = mockk<LoadInventoryQuery>()
    private val sut = InventoryGrpcControllerAdapter(create, decrease, increase, fetchAll, load)

    @Test
    fun `createInventory returns proto response built from command output`() = runBlocking {
        every { create.create(any()) } returns CreateInventoryCommand.Out(
            id = 7, productId = "p", skuCode = "s", quantity = 0
        )

        val response = sut.createInventory(
            CreateInventoryRequest.newBuilder().setProductId("p").setSkuCode("s").build()
        )

        assertEquals(7L, response.id)
    }

    @Test
    fun `decreaseInventory delegates to command and returns updated quantity`() = runBlocking {
        every { decrease.decrease(any()) } returns DecreaseInventoryCommand.Out(
            id = 2, productId = "p", skuCode = "s", quantity = 4
        )

        val response = sut.decreaseInventory(
            DecreaseInventoryRequest.newBuilder().setOrderId(1).setInventoryId(2).setAmount(3).build()
        )

        assertEquals(4, response.quantity)
    }

    @Test
    fun `fetchInventories wraps results in proto response`() = runBlocking {
        every { fetchAll.fetchAll() } returns listOf(
            FetchInventoriesQuery.Out(id = 1, productId = "p", skuCode = "s", quantity = 1),
            FetchInventoriesQuery.Out(id = 2, productId = "p", skuCode = "s", quantity = 2)
        )

        val response = sut.fetchInventories(Empty.newBuilder().build())

        assertEquals(2, response.inventoriesCount)
    }
}
