package dev.ktcloud.black.common.grpc

import io.grpc.Status
import org.springframework.http.HttpStatus

object GrpcStatusMapper {
    fun toGrpcStatus(httpStatus: Int): Status = when (httpStatus) {
        HttpStatus.BAD_REQUEST.value() -> Status.INVALID_ARGUMENT
        HttpStatus.UNAUTHORIZED.value() -> Status.UNAUTHENTICATED
        HttpStatus.FORBIDDEN.value() -> Status.PERMISSION_DENIED
        HttpStatus.NOT_FOUND.value() -> Status.NOT_FOUND
        HttpStatus.CONFLICT.value() -> Status.ALREADY_EXISTS
        HttpStatus.TOO_MANY_REQUESTS.value() -> Status.RESOURCE_EXHAUSTED
        HttpStatus.NOT_IMPLEMENTED.value() -> Status.UNIMPLEMENTED
        HttpStatus.SERVICE_UNAVAILABLE.value() -> Status.UNAVAILABLE
        HttpStatus.GATEWAY_TIMEOUT.value() -> Status.DEADLINE_EXCEEDED
        in 500..599 -> Status.INTERNAL
        else -> Status.UNKNOWN
    }

    fun toHttpStatus(grpcCode: Status.Code): Int = when (grpcCode) {
        Status.Code.OK -> HttpStatus.OK.value()
        Status.Code.INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST.value()
        Status.Code.UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED.value()
        Status.Code.PERMISSION_DENIED -> HttpStatus.FORBIDDEN.value()
        Status.Code.NOT_FOUND -> HttpStatus.NOT_FOUND.value()
        Status.Code.ALREADY_EXISTS -> HttpStatus.CONFLICT.value()
        Status.Code.RESOURCE_EXHAUSTED -> HttpStatus.TOO_MANY_REQUESTS.value()
        Status.Code.UNIMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED.value()
        Status.Code.UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE.value()
        Status.Code.DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT.value()
        Status.Code.FAILED_PRECONDITION,
        Status.Code.OUT_OF_RANGE -> HttpStatus.BAD_REQUEST.value()
        else -> HttpStatus.INTERNAL_SERVER_ERROR.value()
    }
}
