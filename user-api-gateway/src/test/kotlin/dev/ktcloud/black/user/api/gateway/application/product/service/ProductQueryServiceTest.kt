package dev.ktcloud.black.user.api.gateway.application.product.service

import dev.ktcloud.black.product.service.adapter.presentation.web.inbound.grpc.FetchAllProductsResponse
import dev.ktcloud.black.product.service.adapter.presentation.web.inbound.grpc.ProductResponseDto
import dev.ktcloud.black.product.service.adapter.presentation.web.inbound.grpc.ProductServiceGrpcKt
import dev.ktcloud.black.user.api.gateway.application.product.port.inbound.FetchProductQuery
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProductQueryServiceTest {
    private val stub = mockk<ProductServiceGrpcKt.ProductServiceCoroutineStub>()
    private val sut = ProductQueryService(stub)

    @Test
    fun `fetchProduct returns mapped response`() = runBlocking {
        coEvery { stub.fetchProduct(any(), any()) } returns ProductResponseDto.newBuilder()
            .setId("abc").setName("n").setDescription("d").setPrice(5).build()

        val out = sut.fetchProduct(FetchProductQuery.In("abc"))

        assertEquals("abc", out.id)
        assertEquals(5, out.price)
    }

    @Test
    fun `fetchProducts returns mapped list`() = runBlocking {
        coEvery { stub.fetchAll(any(), any()) } returns FetchAllProductsResponse.newBuilder()
            .addProducts(ProductResponseDto.newBuilder().setId("1").setName("a").setDescription("d").setPrice(1))
            .addProducts(ProductResponseDto.newBuilder().setId("2").setName("b").setDescription("d").setPrice(2))
            .build()

        val out = sut.fetchProducts()

        assertEquals(listOf("1", "2"), out.map { it.id })
    }
}
