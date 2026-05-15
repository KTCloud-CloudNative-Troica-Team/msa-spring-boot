package dev.ktcloud.black.common.grpc

import io.grpc.Metadata

object GrpcErrorMetadata {
    val CODE_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-error-code", Metadata.ASCII_STRING_MARSHALLER)

    val HTTP_STATUS_KEY: Metadata.Key<String> =
        Metadata.Key.of("x-error-http-status", Metadata.ASCII_STRING_MARSHALLER)
}
