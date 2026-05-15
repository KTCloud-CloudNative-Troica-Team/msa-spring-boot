package dev.ktcloud.black.inventory.adapter.infrastructure.jpa

import dev.ktcloud.black.inventory.adapter.infrastructure.jpa.entity.Inventory
import dev.ktcloud.black.inventory.domain.entity.InventoryDomainEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InventoryMapperTest {
    private val sut = InventoryMapper()

    @Test
    fun `toOrmEntity copies all fields`() {
        val domain = InventoryDomainEntity(id = 1, productId = "p", skuCode = "s", _quantity = 5)

        val orm = sut.toOrmEntity(domain)

        assertEquals(1, orm.id)
        assertEquals("p", orm.productId)
        assertEquals("s", orm.skuCode)
        assertEquals(5, orm.quantity)
    }

    @Test
    fun `toDomainEntity copies all fields`() {
        val orm = Inventory(id = 2, productId = "p2", skuCode = "s2", quantity = 9)

        val domain = sut.toDomainEntity(orm)

        assertEquals(2, domain.id)
        assertEquals(9, domain.quantity)
    }
}
