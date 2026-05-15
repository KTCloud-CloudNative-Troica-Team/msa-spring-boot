package dev.ktcloud.black.inventory.event.adapter.infrastructure.jpa.repository

import dev.ktcloud.black.inventory.event.adapter.infrastructure.jpa.InventoryEventMapper
import dev.ktcloud.black.inventory.event.adapter.infrastructure.jpa.entity.InventoryEvent
import dev.ktcloud.black.inventory.event.domain.entity.InventoryEventDomainEntity
import dev.ktcloud.black.inventory.event.domain.vo.InventoryEventType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InventoryEventPostgresqlCommandRepositoryTest {
    private val repository = mockk<InventoryEventPostgresqlRepository>()
    private val mapper = mockk<InventoryEventMapper>()
    private val sut = InventoryEventPostgresqlCommandRepository(repository, mapper)

    @Test
    fun `save round-trips through mapper and repository`() {
        val domain = InventoryEventDomainEntity(
            inventoryId = 1, amount = 1, eventType = InventoryEventType.INCREMENT
        )
        val orm = mockk<InventoryEvent>()
        val savedOrm = mockk<InventoryEvent>()
        val savedDomain = domain.copy(id = 99)

        every { mapper.toOrmEntity(domain) } returns orm
        every { repository.save(orm) } returns savedOrm
        every { mapper.toDomainEntity(savedOrm) } returns savedDomain

        val result = sut.save(domain)

        assertEquals(savedDomain, result)
    }

    @Test
    fun `saveAll converts each domain entity through the mapper`() {
        val domains = listOf(
            InventoryEventDomainEntity(inventoryId = 1, amount = 1, eventType = InventoryEventType.INCREMENT),
            InventoryEventDomainEntity(inventoryId = 1, amount = -2, eventType = InventoryEventType.DECREMENT)
        )
        val ormA = mockk<InventoryEvent>()
        val ormB = mockk<InventoryEvent>()
        every { mapper.toOrmEntity(domains[0]) } returns ormA
        every { mapper.toOrmEntity(domains[1]) } returns ormB
        val captured = slot<List<InventoryEvent>>()
        every { repository.saveAll(capture(captured)) } returns emptyList()

        sut.saveAll(domains)

        assertEquals(listOf(ormA, ormB), captured.captured)
        verify(exactly = 1) { repository.saveAll(any<List<InventoryEvent>>()) }
    }
}
