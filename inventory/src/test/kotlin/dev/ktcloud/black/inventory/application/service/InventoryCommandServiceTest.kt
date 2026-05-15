package dev.ktcloud.black.inventory.application.service

import dev.ktcloud.black.client.redis.api.DistributedLock
import dev.ktcloud.black.client.redis.api.IdempotentEventProcessor
import dev.ktcloud.black.inventory.application.dto.event.outbound.InventoryReservedResultEvent
import dev.ktcloud.black.inventory.application.port.event.InventoryOrderEventPublishPort
import dev.ktcloud.black.inventory.application.port.inbound.command.CreateInventoryCommand
import dev.ktcloud.black.inventory.application.port.inbound.command.DecreaseInventoryCommand
import dev.ktcloud.black.inventory.application.port.inbound.command.IncreaseInventoryCommand
import dev.ktcloud.black.inventory.application.port.outbound.LoadCacheSyncedInventoryOutboundPort
import dev.ktcloud.black.inventory.application.port.outbound.UpdateInventoryCommandOutboundPort
import dev.ktcloud.black.inventory.application.port.state.outbound.InventoryStateCommandOutboundPort
import dev.ktcloud.black.inventory.domain.entity.InventoryDomainEntity
import dev.ktcloud.black.inventory.domain.exception.InventoryException
import dev.ktcloud.black.inventory.domain.vo.InventoryReserveResultState
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class InventoryCommandServiceTest {
    private val faker = Faker()
    private val state = mockk<InventoryStateCommandOutboundPort>()
    private val loadAdapter = mockk<LoadCacheSyncedInventoryOutboundPort>()
    private val updateAdapter = mockk<UpdateInventoryCommandOutboundPort>()
    private val distributedLock = mockk<DistributedLock>()
    private val idempotency = mockk<IdempotentEventProcessor>()
    private val publisher = mockk<InventoryOrderEventPublishPort>(relaxed = true)
    private val sut = InventoryCommandService(state, loadAdapter, updateAdapter, distributedLock, idempotency, publisher)

    private fun newInventory(id: Long = 1, qty: Int = 10) = InventoryDomainEntity(
        id = id,
        productId = faker.code().asin(),
        skuCode = faker.code().ean8(),
        _quantity = qty
    )

    private fun bypassLock() {
        val funcSlot = slot<() -> Any?>()
        every {
            distributedLock.execute(any(), capture(funcSlot), any(), any(), any())
        } answers { funcSlot.captured.invoke() }
    }

    @Test
    fun `create persists, reloads through cache, and returns Out`() {
        bypassLock()
        val draft = newInventory(id = 0)
        val saved = newInventory(id = 42, qty = 0)
        every { state.save(any()) } returns saved
        every { loadAdapter.loadCacheSyncedInventory(42L) } returns saved

        val out = sut.create(CreateInventoryCommand.In(productId = saved.productId, skuCode = saved.skuCode))

        assertEquals(42L, out.id)
        assertEquals(saved.productId, out.productId)
    }

    @Test
    fun `decrease publishes SUCCESS and updates inventory quantity on happy path`() {
        bypassLock()
        val inventory = newInventory(id = 5, qty = 7)
        every { loadAdapter.loadInventory(5L) } returns inventory

        val funcSlot = slot<() -> Int>()
        every { idempotency.withIdempotencyProcess<Int>(any(), any(), capture(funcSlot)) } answers { funcSlot.captured.invoke() }
        every { updateAdapter.decrease(5L, 3) } returns 4

        val captured = slot<InventoryReservedResultEvent>()
        every { publisher.publish(capture(captured)) } returns Unit

        val out = sut.decrease(DecreaseInventoryCommand.In(orderId = 100, inventoryId = 5, amount = 3))

        assertEquals(4, out.quantity)
        assertEquals(InventoryReserveResultState.SUCCESS, captured.captured.resultState)
    }

    @Test
    fun `decrease publishes FAILED and rethrows when stock insufficient`() {
        bypassLock()
        val inventory = newInventory(id = 5, qty = 1)
        every { loadAdapter.loadInventory(5L) } returns inventory

        val funcSlot = slot<() -> Int>()
        every { idempotency.withIdempotencyProcess<Int>(any(), any(), capture(funcSlot)) } answers { funcSlot.captured.invoke() }
        every { updateAdapter.decrease(any(), any()) } throws InventoryException.InventoryNotEnough()

        val captured = slot<InventoryReservedResultEvent>()
        every { publisher.publish(capture(captured)) } returns Unit

        assertThrows(InventoryException.InventoryNotEnough::class.java) {
            sut.decrease(DecreaseInventoryCommand.In(orderId = 100, inventoryId = 5, amount = 9))
        }
        assertEquals(InventoryReserveResultState.FAILED, captured.captured.resultState)
    }

    @Test
    fun `decrease falls back to cache rehydration when cache miss`() {
        bypassLock()
        val inventory = newInventory(id = 5, qty = 7)
        every { loadAdapter.loadInventory(5L) } throws InventoryException.NoCachedInventoryFound()
        every { loadAdapter.loadCacheSyncedInventory(5L) } returns inventory

        val funcSlot = slot<() -> Int>()
        every { idempotency.withIdempotencyProcess<Int>(any(), any(), capture(funcSlot)) } answers { funcSlot.captured.invoke() }
        every { updateAdapter.decrease(5L, 2) } returns 5

        val out = sut.decrease(DecreaseInventoryCommand.In(orderId = 1, inventoryId = 5, amount = 2))

        assertEquals(5, out.quantity)
        verify { loadAdapter.loadCacheSyncedInventory(5L) }
    }

    @Test
    fun `increase updates the in-memory quantity`() {
        val inventory = newInventory(id = 5, qty = 3)
        every { loadAdapter.loadInventory(5L) } returns inventory
        every { updateAdapter.increase(5L, 4) } returns 7

        val out = sut.increase(IncreaseInventoryCommand.In(inventoryId = 5, amount = 4))

        assertEquals(7, out.quantity)
    }
}
