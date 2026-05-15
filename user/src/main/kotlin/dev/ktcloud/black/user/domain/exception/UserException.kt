package dev.ktcloud.black.user.domain.exception

import dev.ktcloud.black.common.exception.CustomException
import dev.ktcloud.black.common.exception.HttpStatusCode

sealed class UserException {
    class UserNotFoundException(message: String? = null, e: Throwable? = null) : CustomException(
        "001",
        message ?: "ユーザーが見つかりません",
        HttpStatusCode.NOT_FOUND,
        e
    )
}
