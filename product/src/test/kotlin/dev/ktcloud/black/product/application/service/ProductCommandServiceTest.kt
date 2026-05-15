package dev.ktcloud.black.product.application.service

import dev.ktcloud.black.product.application.port.inbound.CreateProductCommand
import dev.ktcloud.black.product.application.port.outbound.ProductCommandOutboundPort
import dev.ktcloud.black.product.domain.entity.ProductDomainEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import net.datafaker.Faker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class ProductCommandServiceTest {
    private val faker = Faker()
    private val commandPort = mockk<ProductCommandOutboundPort>()
    private val sut = ProductCommandService(commandPort)

    @Test
    fun `createProduct persists and returns Out with id from saved entity`() {
        val name = faker.commerce().productName()
        val captured = slot<ProductDomainEntity>()
        val savedId = UUID.randomUUID()
        every { commandPort.save(capture(captured)) } answers {
            captured.captured.copy(id = savedId)
        }

        val out = sut.createProduct(
            CreateProductCommand.In(name = name, description = "desc", price = 1500)
        )

        assertEquals(savedId.toString(), out.id)
        assertEquals(name, out.name)
        assertEquals(1500, out.price)
    }
}
