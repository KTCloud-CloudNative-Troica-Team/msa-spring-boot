package dev.ktcloud.black.common.exception

open class CustomException(
    val code: String,
    override val message: String,
    val status: Int,
    val throwable: Throwable? = null,
) : RuntimeException(message, throwable)

data class ExceptionBody(
    val code: String,
    val message: String,
    val status: Int,
)

object HttpStatusCode {
    const val BAD_REQUEST = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val CONFLICT = 409
    const val TOO_MANY_REQUESTS = 429
    const val INTERNAL_SERVER_ERROR = 500
    const val SERVICE_UNAVAILABLE = 503
}
