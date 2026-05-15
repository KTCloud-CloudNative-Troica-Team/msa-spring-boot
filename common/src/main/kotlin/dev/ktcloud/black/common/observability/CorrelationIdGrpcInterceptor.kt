package dev.ktcloud.black.common.observability

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.UUID

object CorrelationIdConstants {
    const val HEADER_NAME = "x-correlation-id"
    const val MDC_KEY = "correlationId"
    val METADATA_KEY: Metadata.Key<String> = Metadata.Key.of(HEADER_NAME, Metadata.ASCII_STRING_MARSHALLER)
    val GRPC_CONTEXT_KEY: Context.Key<String> = Context.key(MDC_KEY)
}

@Component
@GrpcGlobalServerInterceptor
class CorrelationIdGrpcInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val incoming = headers.get(CorrelationIdConstants.METADATA_KEY)
        val correlationId = incoming ?: UUID.randomUUID().toString()

        MDC.put(CorrelationIdConstants.MDC_KEY, correlationId)
        try {
            val ctx = Context.current().withValue(CorrelationIdConstants.GRPC_CONTEXT_KEY, correlationId)
            return Contexts.interceptCall(ctx, call, headers, next)
        } finally {
            MDC.remove(CorrelationIdConstants.MDC_KEY)
        }
    }
}
