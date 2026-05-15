package dev.ktcloud.black.user.api.gateway.application.inventory.service

import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.FetchInventoriesResponse
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.InventoryResponseDto
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.InventoryServiceGrpcKt
import dev.ktcloud.black.user.api.gateway.application.inventory.port.inbound.FetchInventoryQuery
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InventoryQueryServiceTest {
    private val stub = mockk<InventoryServiceGrpcKt.InventoryServiceCoroutineStub>()
    private val sut = InventoryQueryService(stub)

    @Test
    fun `fetchInventory maps proto response to Out`() = runBlocking {
        coEvery { stub.fetchInventory(any(), any()) } returns InventoryResponseDto.newBuilder()
            .setId(7).setProductId("p").setSkuCode("s").setQuantity(4).build()

        val out = sut.fetchInventory(FetchInventoryQuery.In(7L))

        assertEquals(7L, out.id)
        assertEquals(4, out.quantity)
    }

    @Test
    fun `fetchAll returns mapped list`() = runBlocking {
        coEvery { stub.fetchInventories(any(), any()) } returns FetchInventoriesResponse.newBuilder()
            .addInventories(InventoryResponseDto.newBuilder().setId(1).setQuantity(1).build())
            .addInventories(InventoryResponseDto.newBuilder().setId(2).setQuantity(2).build())
            .build()

        val out = sut.fetchAll()

        assertEquals(listOf(1L, 2L), out.map { it.id })
    }
}
