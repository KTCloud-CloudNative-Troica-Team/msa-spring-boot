package dev.ktcloud.black.common.adapter.infrastructure.jpa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

private class SoftDeletableOrm : BaseOrmEntity()

class BaseOrmEntityTest {
    @Test
    fun `delete sets deletedAt to provided time`() {
        val entity = SoftDeletableOrm()
        val time = LocalDateTime.of(2026, 1, 1, 12, 0)

        entity.delete(time)

        assertEquals(time, entity.deletedAt)
    }

    @Test
    fun `default deletedAt is null`() {
        val entity = SoftDeletableOrm()

        assertNull(entity.deletedAt)
    }
}
