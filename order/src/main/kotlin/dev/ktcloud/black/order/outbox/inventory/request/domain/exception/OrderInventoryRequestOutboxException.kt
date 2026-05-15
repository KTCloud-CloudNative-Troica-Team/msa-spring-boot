package dev.ktcloud.black.order.outbox.inventory.request.domain.exception

import dev.ktcloud.black.common.exception.CustomException
import dev.ktcloud.black.common.exception.HttpStatusCode

sealed class OrderInventoryRequestOutboxException {
    class IllegalStatusTransitive(message: String? = null, e: Throwable? = null) : CustomException(
        "001",
        message ?: "間違えたステータス転移です",
        HttpStatusCode.BAD_REQUEST,
        e
    )

    class OutboxFailedOverMaximum(message: String? = null, e: Throwable? = null) : CustomException(
        "002",
        message ?: "制限を超えた失敗数です",
        HttpStatusCode.BAD_REQUEST,
        e
    )
}
