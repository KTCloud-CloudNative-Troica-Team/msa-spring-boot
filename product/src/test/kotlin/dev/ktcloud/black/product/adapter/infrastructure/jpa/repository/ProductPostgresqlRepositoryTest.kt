package dev.ktcloud.black.product.adapter.infrastructure.jpa.repository

import dev.ktcloud.black.product.adapter.infrastructure.jpa.ProductMapper
import dev.ktcloud.black.product.adapter.infrastructure.jpa.entity.Product
import dev.ktcloud.black.product.domain.entity.ProductDomainEntity
import dev.ktcloud.black.product.domain.exception.ProductException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class ProductPostgresqlRepositoryTest {
    private val repo = mockk<ProductPostgresqlRepository>()
    private val mapper = mockk<ProductMapper>()
    private val command = ProductPostgresqlCommandRepository(repo, mapper)
    private val query = ProductPostgresqlQueryRepository(repo, mapper)

    @Test
    fun `save round-trips through mapper`() {
        val domain = ProductDomainEntity(name = "n", description = "d", price = 9)
        val orm = mockk<Product>()
        val savedOrm = mockk<Product>()
        every { mapper.toOrmEntity(domain) } returns orm
        every { repo.save(orm) } returns savedOrm
        every { mapper.toDomainEntity(savedOrm) } returns domain

        assertEquals(domain, command.save(domain))
    }

    @Test
    fun `fetch by id throws NoSuchProductException when missing`() {
        val id = UUID.randomUUID()
        every { repo.findById(id) } returns Optional.empty()

        assertThrows(ProductException.NoSuchProductException::class.java) {
            query.fetch(id.toString())
        }
    }
}
