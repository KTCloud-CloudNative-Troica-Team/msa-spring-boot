package dev.ktcloud.black.user.application.service

import dev.ktcloud.black.user.application.port.inbound.CreateUserCommand
import dev.ktcloud.black.user.application.port.outbound.UserCommandOutboundPort
import dev.ktcloud.black.user.domain.entity.UserDomainEntity
import dev.ktcloud.black.user.domain.vo.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import net.datafaker.Faker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder

class UserCommandServiceTest {
    private val faker = Faker()
    private val commandPort = mockk<UserCommandOutboundPort>()
    private val encoder = mockk<PasswordEncoder>()
    private val sut = UserCommandService(commandPort, encoder)

    @Test
    fun `create encodes password and persists user with USER role`() {
        val email = faker.internet().emailAddress()
        every { encoder.encode("plain") } returns "ENCODED"
        val captured = slot<UserDomainEntity>()
        every { commandPort.save(capture(captured)) } answers { captured.captured }

        val out = sut.create(CreateUserCommand.In(email = email, plainPassword = "plain", name = "K"))

        assertEquals("ENCODED", captured.captured.password)
        assertEquals(UserRole.USER, captured.captured.role)
        assertEquals(email, out.email)
    }
}
