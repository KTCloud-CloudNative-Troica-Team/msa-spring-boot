package dev.ktcloud.black.product.application.service

import dev.ktcloud.black.product.application.port.inbound.FetchProductQuery
import dev.ktcloud.black.product.application.port.outbound.ProductQueryOutboundPort
import dev.ktcloud.black.product.domain.entity.ProductDomainEntity
import dev.ktcloud.black.product.domain.exception.ProductException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class ProductQueryServiceTest {
    private val queryPort = mockk<ProductQueryOutboundPort>()
    private val sut = ProductQueryService(queryPort)

    @Test
    fun `fetchAll maps each product into Out`() {
        every { queryPort.fetchAll() } returns listOf(
            ProductDomainEntity(name = "a", description = "d", price = 1),
            ProductDomainEntity(name = "b", description = "d", price = 2),
        )

        val out = sut.fetchAll()

        assertEquals(listOf("a", "b"), out.map { it.name })
    }

    @Test
    fun `fetch returns Out for the requested id`() {
        val id = UUID.randomUUID()
        val product = ProductDomainEntity(id = id, name = "x", description = "d", price = 9)
        every { queryPort.fetch(id.toString()) } returns product

        val out = sut.fetch(FetchProductQuery.In(id.toString()))

        assertEquals(id.toString(), out.id)
        assertEquals("x", out.name)
    }

    @Test
    fun `fetch propagates NoSuchProductException from outbound port`() {
        every { queryPort.fetch("missing") } throws ProductException.NoSuchProductException()

        assertThrows(ProductException.NoSuchProductException::class.java) {
            sut.fetch(FetchProductQuery.In("missing"))
        }
    }
}
