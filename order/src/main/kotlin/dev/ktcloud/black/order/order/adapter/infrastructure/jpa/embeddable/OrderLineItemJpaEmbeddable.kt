package dev.ktcloud.black.order.order.adapter.infrastructure.jpa.embeddable

import dev.ktcloud.black.order.order.domain.vo.OrderLineItem
import dev.ktcloud.black.order.order.domain.vo.OrderLineItemStatus
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class OrderLineItemJpaEmbeddable(
    @Column(name = "inventory_id", nullable = false)
    val inventoryId: Long = 0L,

    @Column(name = "product_id", nullable = false)
    val productId: String = "",

    @Column(name = "sku_code", nullable = false)
    val skuCode: String = "",

    @Column(name = "price", nullable = false)
    val price: Int = 0,

    @Column(name = "quantity", nullable = false)
    val quantity: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: OrderLineItemStatus = OrderLineItemStatus.PENDING,
) {
    fun toDomain(): OrderLineItem = OrderLineItem(
        inventoryId = inventoryId,
        productId = productId,
        skuCode = skuCode,
        price = price,
        quantity = quantity,
        status = status,
    )

    companion object {
        fun fromDomain(item: OrderLineItem): OrderLineItemJpaEmbeddable = OrderLineItemJpaEmbeddable(
            inventoryId = item.inventoryId,
            productId = item.productId,
            skuCode = item.skuCode,
            price = item.price,
            quantity = item.quantity,
            status = item.status,
        )
    }
}
