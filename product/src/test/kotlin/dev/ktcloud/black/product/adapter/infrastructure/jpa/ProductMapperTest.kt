package dev.ktcloud.black.product.adapter.infrastructure.jpa

import dev.ktcloud.black.product.adapter.infrastructure.jpa.entity.Product
import dev.ktcloud.black.product.domain.entity.ProductDomainEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class ProductMapperTest {
    private val sut = ProductMapper()

    @Test
    fun `toOrmEntity copies all fields`() {
        val id = UUID.randomUUID()
        val orm = sut.toOrmEntity(ProductDomainEntity(id = id, name = "n", description = "d", price = 100))

        assertEquals(id, orm.id)
        assertEquals("n", orm.name)
        assertEquals(100, orm.price)
    }

    @Test
    fun `toDomainEntity copies all fields`() {
        val id = UUID.randomUUID()
        val domain = sut.toDomainEntity(Product(id = id, name = "n", description = "d", price = 100))

        assertEquals(id, domain.id)
        assertEquals("n", domain.name)
    }
}
