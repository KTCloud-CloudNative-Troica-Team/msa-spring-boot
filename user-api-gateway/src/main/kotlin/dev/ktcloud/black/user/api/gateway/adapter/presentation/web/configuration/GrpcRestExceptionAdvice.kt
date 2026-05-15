package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.configuration

import dev.ktcloud.black.common.exception.CustomException
import dev.ktcloud.black.common.exception.ExceptionBody
import dev.ktcloud.black.common.grpc.GrpcErrorMetadata
import dev.ktcloud.black.common.grpc.GrpcStatusMapper
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GrpcRestExceptionAdvice {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(e: CustomException): ResponseEntity<ExceptionBody> {
        log.warn("CustomException at gateway: code={}, status={}, message={}", e.code, e.status, e.message)
        return ResponseEntity.status(e.status)
            .body(ExceptionBody(code = e.code, message = e.message, status = e.status))
    }

    @ExceptionHandler(StatusRuntimeException::class)
    fun handleStatusRuntimeException(e: StatusRuntimeException): ResponseEntity<ExceptionBody> =
        toResponseEntity(e.status, e.trailers, e)

    @ExceptionHandler(StatusException::class)
    fun handleStatusException(e: StatusException): ResponseEntity<ExceptionBody> =
        toResponseEntity(e.status, e.trailers, e)

    private fun toResponseEntity(
        grpcStatus: io.grpc.Status,
        trailers: io.grpc.Metadata?,
        e: Throwable
    ): ResponseEntity<ExceptionBody> {
        val code = trailers?.get(GrpcErrorMetadata.CODE_KEY) ?: UNMAPPED_CODE
        val httpStatus = trailers?.get(GrpcErrorMetadata.HTTP_STATUS_KEY)?.toIntOrNull()
            ?: GrpcStatusMapper.toHttpStatus(grpcStatus.code)
        val message = grpcStatus.description ?: e.message ?: grpcStatus.code.name

        log.warn(
            "gRPC error at gateway: grpcCode={}, httpStatus={}, code={}, message={}",
            grpcStatus.code, httpStatus, code, message
        )

        return ResponseEntity.status(httpStatus).body(
            ExceptionBody(code = code, message = message, status = httpStatus)
        )
    }

    companion object {
        private const val UNMAPPED_CODE = "GRPC_UNMAPPED"
    }
}
