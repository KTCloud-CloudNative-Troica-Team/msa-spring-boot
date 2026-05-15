package dev.ktcloud.black.inventory.adapter.infrastructure.jpa.repository

import dev.ktcloud.black.inventory.adapter.infrastructure.jpa.InventoryMapper
import dev.ktcloud.black.inventory.adapter.infrastructure.jpa.entity.Inventory
import dev.ktcloud.black.inventory.domain.entity.InventoryDomainEntity
import dev.ktcloud.black.inventory.domain.exception.InventoryException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Optional

class InventoryPostgresqlQueryRepositoryTest {
    private val repo = mockk<InventoryPostgresqlRepository>()
    private val mapper = mockk<InventoryMapper>()
    private val sut = InventoryPostgresqlQueryRepository(repo, mapper)

    @Test
    fun `fetch by id returns mapped domain entity`() {
        val orm = mockk<Inventory>()
        val domain = InventoryDomainEntity(id = 1, productId = "p", skuCode = "s")
        every { repo.findById(1L) } returns Optional.of(orm)
        every { mapper.toDomainEntity(orm) } returns domain

        val result = sut.fetch(1L)

        assertEquals(domain, result)
    }

    @Test
    fun `fetch by id throws NoSuchInventory when missing`() {
        every { repo.findById(99L) } returns Optional.empty()

        assertThrows(InventoryException.NoSuchInventory::class.java) { sut.fetch(99L) }
    }

    @Test
    fun `fetch by productId+sku throws when missing`() {
        every { repo.findByProductIdAndSkuCode("p", "s") } returns Optional.empty()

        assertThrows(InventoryException.NoSuchInventory::class.java) { sut.fetch("p", "s") }
    }
}
