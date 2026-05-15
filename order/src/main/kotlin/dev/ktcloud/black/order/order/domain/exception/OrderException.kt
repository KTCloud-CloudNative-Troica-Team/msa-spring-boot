package dev.ktcloud.black.order.order.domain.exception

import dev.ktcloud.black.common.exception.CustomException
import dev.ktcloud.black.common.exception.HttpStatusCode

sealed class OrderException {
    class IllegalOrderStatusTransitive(message: String? = null, e: Throwable? = null) : CustomException(
        "001",
        message ?: "間違えたステータス転移です",
        HttpStatusCode.BAD_REQUEST,
        e
    )

    class NoSuchOrder(message: String? = null, e: Throwable? = null) : CustomException(
        "002",
        message ?: "存在しない注文です",
        HttpStatusCode.NOT_FOUND,
        e
    )

    class OrderStatusUpdateImpossible(message: String? = null, e: Throwable? = null) : CustomException(
        "003",
        message ?: "注文の　不可能な状態の更新です",
        HttpStatusCode.BAD_REQUEST,
        e
    )
}
