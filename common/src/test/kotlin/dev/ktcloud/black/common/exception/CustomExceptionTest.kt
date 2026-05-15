package dev.ktcloud.black.common.exception

import net.datafaker.Faker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class CustomExceptionTest {
    private val faker = Faker()

    @Test
    fun `carries code, message, and status as plain values`() {
        val code = faker.numerify("###")
        val message = faker.lorem().sentence()
        val ex = CustomException(code, message, HttpStatusCode.BAD_REQUEST)

        assertEquals(code, ex.code)
        assertEquals(message, ex.message)
        assertEquals(HttpStatusCode.BAD_REQUEST, ex.status)
    }

    @Test
    fun `preserves cause chain when supplied`() {
        val cause = RuntimeException("boom")
        val ex = CustomException("X", "fail", HttpStatusCode.INTERNAL_SERVER_ERROR, cause)

        assertSame(cause, ex.throwable)
        assertSame(cause, ex.cause)
    }
}
