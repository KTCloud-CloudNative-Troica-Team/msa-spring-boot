package dev.ktcloud.black.auth.domain.exception

import dev.ktcloud.black.common.exception.CustomException
import dev.ktcloud.black.common.exception.HttpStatusCode

sealed class AuthException {
    class RefreshTokenFail(message: String? = null, e: Throwable? = null) : CustomException(
        "001",
        message ?: "トークン更新に失敗しました",
        HttpStatusCode.UNAUTHORIZED,
        e,
    )

    class ExpiredRefreshToken(message: String? = null, e: Throwable? = null) : CustomException(
        "002",
        message ?: "満了された更新トークンです",
        HttpStatusCode.UNAUTHORIZED,
        e,
    )

    class ExpiredAccessToken(message: String? = null, e: Throwable? = null) : CustomException(
        "003",
        message ?: "満了されたアクセストークンです",
        HttpStatusCode.UNAUTHORIZED,
        e,
    )
}
