package dev.ktcloud.black.common.adapter.infrastructure.kafka

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private data class FakeMessage(val value: String)
private data class FakeEvent(val value: String)

private class FakeEventMapper : EventMapper<FakeMessage, FakeEvent> {
    override fun toMessage(event: FakeEvent) = FakeMessage(event.value)
    override fun toEvent(message: FakeMessage) = FakeEvent(message.value)
}

class EventMapperTest {
    private val mapper = FakeEventMapper()

    @Test
    fun `default list overload converts events to messages`() {
        val messages = mapper.toMessage(listOf(FakeEvent("a"), FakeEvent("b")))

        assertEquals(listOf("a", "b"), messages.map { it.value })
    }

    @Test
    fun `default list overload converts messages to events`() {
        val events = mapper.toEvent(listOf(FakeMessage("x"), FakeMessage("y")))

        assertEquals(listOf("x", "y"), events.map { it.value })
    }
}
