package dev.ktcloud.black.product.application.service

import dev.ktcloud.black.product.application.port.inbound.CreateProductCommand
import dev.ktcloud.black.product.application.port.outbound.ProductCommandOutboundPort
import dev.ktcloud.black.product.domain.entity.ProductDomainEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductCommandService(
    private val productCommandOutboundPort: ProductCommandOutboundPort,
): CreateProductCommand {
    @Transactional
    override fun createProduct(command: CreateProductCommand.In): CreateProductCommand.Out {
        val productDomainEntity = ProductDomainEntity(
            name = command.name,
            description = command.description,
            price = command.price,
        )

        val saved = productCommandOutboundPort.save(productDomainEntity)

        return CreateProductCommand.Out(
            id = saved.id.toString(),
            name = saved.name,
            description = saved.description,
            price = saved.price,
        )
    }
}