package dev.ktcloud.black.common.grpc

import dev.ktcloud.black.common.exception.CustomException
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.Status
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class GrpcExceptionMappingInterceptorTest {
    private val interceptor = GrpcExceptionMappingInterceptor()

    @Test
    fun `maps CustomException cause to gRPC status with metadata`() {
        val call = mockk<ServerCall<String, String>>(relaxed = true)
        val handler = mockk<ServerCallHandler<String, String>>(relaxed = true)

        val wrappedSlot = slot<ServerCall<String, String>>()
        every { handler.startCall(capture(wrappedSlot), any()) } returns mockk(relaxed = true)

        interceptor.interceptCall(call, Metadata(), handler)

        val custom = CustomException("999", "boom", HttpStatus.NOT_FOUND.value())
        val status = Status.UNKNOWN.withCause(custom)
        wrappedSlot.captured.close(status, Metadata())

        verify {
            call.close(
                match { it.code == Status.NOT_FOUND.code && it.description == "boom" },
                match {
                    it.get(GrpcErrorMetadata.CODE_KEY) == "999" &&
                        it.get(GrpcErrorMetadata.HTTP_STATUS_KEY) == HttpStatus.NOT_FOUND.value().toString()
                }
            )
        }
    }

    @Test
    fun `passes through non-custom errors untouched`() {
        val call = mockk<ServerCall<String, String>>(relaxed = true)
        val handler = mockk<ServerCallHandler<String, String>>(relaxed = true)

        val wrappedSlot = slot<ServerCall<String, String>>()
        every { handler.startCall(capture(wrappedSlot), any()) } returns mockk(relaxed = true)

        interceptor.interceptCall(call, Metadata(), handler)

        val originalStatus = Status.INTERNAL.withDescription("plain")
        val originalTrailers = Metadata()
        wrappedSlot.captured.close(originalStatus, originalTrailers)

        verify { call.close(originalStatus, originalTrailers) }
    }

    @Test
    fun `unwraps CustomException nested in cause chain`() {
        val call = mockk<ServerCall<String, String>>(relaxed = true)
        val handler = mockk<ServerCallHandler<String, String>>(relaxed = true)
        val wrappedSlot = slot<ServerCall<String, String>>()
        every { handler.startCall(capture(wrappedSlot), any()) } returns mockk(relaxed = true)

        interceptor.interceptCall(call, Metadata(), handler)

        val nested = CustomException("123", "wrapped", HttpStatus.CONFLICT.value())
        val outer = RuntimeException("outer", nested)
        wrappedSlot.captured.close(Status.UNKNOWN.withCause(outer), Metadata())

        verify {
            call.close(
                match { it.code == Status.ALREADY_EXISTS.code },
                any()
            )
        }
    }
}
