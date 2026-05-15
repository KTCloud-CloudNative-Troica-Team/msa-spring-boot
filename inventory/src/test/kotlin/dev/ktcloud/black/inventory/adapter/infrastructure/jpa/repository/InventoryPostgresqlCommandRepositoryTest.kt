package dev.ktcloud.black.inventory.adapter.infrastructure.jpa.repository

import dev.ktcloud.black.inventory.adapter.infrastructure.jpa.InventoryMapper
import dev.ktcloud.black.inventory.adapter.infrastructure.jpa.entity.Inventory
import dev.ktcloud.black.inventory.domain.entity.InventoryDomainEntity
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InventoryPostgresqlCommandRepositoryTest {
    private val repo = mockk<InventoryPostgresqlRepository>()
    private val mapper = mockk<InventoryMapper>()
    private val sut = InventoryPostgresqlCommandRepository(repo, mapper)

    @Test
    fun `save round-trips through mapper`() {
        val domain = InventoryDomainEntity(productId = "p", skuCode = "s")
        val orm = mockk<Inventory>()
        val savedOrm = mockk<Inventory>()
        val savedDomain = domain.copy(id = 99)

        every { mapper.toOrmEntity(domain) } returns orm
        every { repo.save(orm) } returns savedOrm
        every { mapper.toDomainEntity(savedOrm) } returns savedDomain

        val result = sut.save(domain)

        assertEquals(savedDomain, result)
    }
}
