package dev.ktcloud.black.inventory.event.adapter.infrastructure.jpa

import dev.ktcloud.black.inventory.event.adapter.infrastructure.jpa.entity.InventoryEvent
import dev.ktcloud.black.inventory.event.domain.entity.InventoryEventDomainEntity
import dev.ktcloud.black.inventory.event.domain.vo.InventoryEventProcessStatus
import dev.ktcloud.black.inventory.event.domain.vo.InventoryEventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InventoryEventMapperTest {
    private val sut = InventoryEventMapper()

    @Test
    fun `toOrmEntity copies all fields`() {
        val domain = InventoryEventDomainEntity(
            id = 3,
            inventoryId = 10,
            amount = -2,
            eventType = InventoryEventType.DECREMENT
        )

        val orm = sut.toOrmEntity(domain)

        assertEquals(3, orm.id)
        assertEquals(10, orm.inventoryId)
        assertEquals(-2, orm.amount)
        assertEquals(InventoryEventType.DECREMENT, orm.eventType)
        assertEquals(InventoryEventProcessStatus.PENDING, orm.processStatus)
    }

    @Test
    fun `toDomainEntity copies all fields including process status`() {
        val orm = InventoryEvent(
            id = 4,
            inventoryId = 11,
            amount = 7,
            eventType = InventoryEventType.INCREMENT,
            processStatus = InventoryEventProcessStatus.PROCESSED
        )

        val domain = sut.toDomainEntity(orm)

        assertEquals(4, domain.id)
        assertEquals(InventoryEventProcessStatus.PROCESSED, domain.processStatus)
    }
}
