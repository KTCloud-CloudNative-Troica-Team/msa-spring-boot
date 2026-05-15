package dev.ktcloud.black.common.adapter.infrastructure.jpa

import dev.ktcloud.black.common.adapter.infrastructure.jpa.BaseOrmEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private data class FakeOrm(val value: String) : BaseOrmEntity()
private data class FakeDomain(val value: String)

private class FakeMapper : EntityMapper<FakeOrm, FakeDomain> {
    override fun toOrmEntity(domainEntity: FakeDomain) = FakeOrm(domainEntity.value)
    override fun toDomainEntity(entity: FakeOrm) = FakeDomain(entity.value)
}

class EntityMapperTest {
    private val mapper = FakeMapper()

    @Test
    fun `default list overload converts each item`() {
        val ormList = mapper.toOrmEntity(listOf(FakeDomain("a"), FakeDomain("b")))

        assertEquals(listOf("a", "b"), ormList.map { it.value })
    }

    @Test
    fun `default Iterable overload converts each item`() {
        val sequence: Iterable<FakeOrm> = listOf(FakeOrm("x"), FakeOrm("y"))

        val domains = mapper.toDomainEntity(sequence)

        assertEquals(listOf("x", "y"), domains.map { it.value })
    }
}
