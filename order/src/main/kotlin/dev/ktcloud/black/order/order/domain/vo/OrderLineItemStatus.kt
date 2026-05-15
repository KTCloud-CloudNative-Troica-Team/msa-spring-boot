package dev.ktcloud.black.order.order.domain.vo

enum class OrderLineItemStatus {
    PENDING, INVENTORY_RESERVED, INVENTORY_RELEASED, FAILED;

    private fun getTransitiveList(): List<OrderLineItemStatus> {
        return when (this) {
            PENDING -> listOf(INVENTORY_RESERVED, FAILED)
            INVENTORY_RESERVED -> listOf(INVENTORY_RELEASED, FAILED)
            INVENTORY_RELEASED -> listOf()
            FAILED -> listOf()
        }
    }

    fun checkTransitive(orderStatus: OrderLineItemStatus): Boolean {
        return getTransitiveList().contains(orderStatus)
    }
}
