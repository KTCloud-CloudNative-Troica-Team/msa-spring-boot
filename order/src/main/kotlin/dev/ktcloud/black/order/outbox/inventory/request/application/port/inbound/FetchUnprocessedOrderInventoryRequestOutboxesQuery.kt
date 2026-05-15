package dev.ktcloud.black.order.outbox.inventory.request.application.port.inbound

import dev.ktcloud.black.order.outbox.inventory.request.domain.vo.OrderInventoryRequestOutboxStatus

interface FetchUnprocessedOrderInventoryRequestOutboxesQuery {
    fun fetchAllUnprocessed(): List<Out>

    data class Out(
        val id: Long,
        val status: OrderInventoryRequestOutboxStatus,
        val orderId: Long,
        val inventoryId: Long,
        val amount: Int
    )
}