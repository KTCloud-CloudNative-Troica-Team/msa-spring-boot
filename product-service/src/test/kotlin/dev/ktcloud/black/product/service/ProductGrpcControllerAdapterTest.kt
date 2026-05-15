package dev.ktcloud.black.product.service

import dev.ktcloud.black.product.application.port.inbound.CreateProductCommand
import dev.ktcloud.black.product.application.port.inbound.FetchAllProductsQuery
import dev.ktcloud.black.product.application.port.inbound.FetchProductQuery
import dev.ktcloud.black.product.service.adapter.presentation.web.inbound.grpc.CreateProductRequest
import dev.ktcloud.black.product.service.adapter.presentation.web.inbound.grpc.Empty
import dev.ktcloud.black.product.service.adapter.presentation.web.inbound.grpc.FetchProductRequest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProductGrpcControllerAdapterTest {
    private val createProduct = mockk<CreateProductCommand>()
    private val fetchAll = mockk<FetchAllProductsQuery>()
    private val fetchOne = mockk<FetchProductQuery>()
    private val sut = ProductGrpcControllerAdapter(createProduct, fetchAll, fetchOne)

    @Test
    fun `createProduct maps proto request to command and back to proto response`() = runBlocking {
        every { createProduct.createProduct(any()) } returns CreateProductCommand.Out(
            id = "abc", name = "n", description = "d", price = 5
        )

        val response = sut.createProduct(
            CreateProductRequest.newBuilder().setName("n").setDescription("d").setPrice(5).build()
        )

        assertEquals("abc", response.id)
        assertEquals(5, response.price)
    }

    @Test
    fun `fetchProduct delegates to query and translates to proto`() = runBlocking {
        every { fetchOne.fetch(FetchProductQuery.In("xyz")) } returns FetchProductQuery.Out(
            id = "xyz", name = "n", description = "d", price = 1
        )

        val response = sut.fetchProduct(FetchProductRequest.newBuilder().setId("xyz").build())

        assertEquals("xyz", response.id)
    }

    @Test
    fun `fetchAll wraps each result inside the response builder`() = runBlocking {
        every { fetchAll.fetchAll() } returns listOf(
            FetchAllProductsQuery.Out(id = "1", name = "a", description = "d", price = 1),
            FetchAllProductsQuery.Out(id = "2", name = "b", description = "d", price = 2)
        )

        val response = sut.fetchAll(Empty.newBuilder().build())

        assertEquals(2, response.productsCount)
    }
}
