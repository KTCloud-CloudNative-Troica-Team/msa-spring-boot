package dev.ktcloud.black.common.grpc

import dev.ktcloud.black.common.exception.CustomException
import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@GrpcGlobalServerInterceptor
class GrpcExceptionMappingInterceptor : ServerInterceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val method = call.methodDescriptor.fullMethodName
        val wrapped = object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            override fun close(status: Status, trailers: Metadata) {
                val custom = status.cause?.findCustomException()
                if (custom != null) {
                    val mapped = GrpcStatusMapper.toGrpcStatus(custom.status)
                        .withDescription(custom.message)
                        .withCause(custom)
                    val mappedTrailers = Metadata().apply {
                        merge(trailers)
                        put(GrpcErrorMetadata.CODE_KEY, custom.code)
                        put(GrpcErrorMetadata.HTTP_STATUS_KEY, custom.status.toString())
                    }
                    log.warn(
                        "gRPC {} CustomException code={} httpStatus={} message={}",
                        method, custom.code, custom.status, custom.message, custom
                    )
                    super.close(mapped, mappedTrailers)
                } else {
                    if (!status.isOk) {
                        log.error(
                            "gRPC {} unmapped error: grpcCode={} description={}",
                            method, status.code, status.description, status.cause
                        )
                    }
                    super.close(status, trailers)
                }
            }
        }
        return next.startCall(wrapped, headers)
    }

    private fun Throwable.findCustomException(): CustomException? {
        var current: Throwable? = this
        val seen = mutableSetOf<Throwable>()
        while (current != null && seen.add(current)) {
            if (current is CustomException) return current
            current = current.cause
        }
        return null
    }
}
