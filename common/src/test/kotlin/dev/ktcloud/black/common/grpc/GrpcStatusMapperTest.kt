package dev.ktcloud.black.common.grpc

import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class GrpcStatusMapperTest {
    @Test
    fun `maps known HTTP status to gRPC status`() {
        assertEquals(Status.INVALID_ARGUMENT.code, GrpcStatusMapper.toGrpcStatus(HttpStatus.BAD_REQUEST.value()).code)
        assertEquals(Status.NOT_FOUND.code, GrpcStatusMapper.toGrpcStatus(HttpStatus.NOT_FOUND.value()).code)
        assertEquals(Status.INTERNAL.code, GrpcStatusMapper.toGrpcStatus(HttpStatus.INTERNAL_SERVER_ERROR.value()).code)
    }

    @Test
    fun `maps unknown HTTP status to UNKNOWN`() {
        assertEquals(Status.UNKNOWN.code, GrpcStatusMapper.toGrpcStatus(700).code)
    }

    @Test
    fun `maps known gRPC status to HTTP`() {
        assertEquals(HttpStatus.NOT_FOUND.value(), GrpcStatusMapper.toHttpStatus(Status.Code.NOT_FOUND))
        assertEquals(HttpStatus.BAD_REQUEST.value(), GrpcStatusMapper.toHttpStatus(Status.Code.FAILED_PRECONDITION))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), GrpcStatusMapper.toHttpStatus(Status.Code.UNKNOWN))
    }
}
