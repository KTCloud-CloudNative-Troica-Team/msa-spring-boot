package dev.ktcloud.black.admin.api.gateway.application.product.service

import dev.ktcloud.black.admin.api.gateway.application.product.port.inbound.CreateProductCommand
import dev.ktcloud.black.product.service.adapter.presentation.web.inbound.grpc.CreateProductRequest
import dev.ktcloud.black.product.service.adapter.presentation.web.inbound.grpc.ProductServiceGrpcKt
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service

@Service
class ProductCommandService(
    @GrpcClient("product-service")
    private val productServiceStub: ProductServiceGrpcKt.ProductServiceCoroutineStub
) : CreateProductCommand {
    override suspend fun createProduct(command: CreateProductCommand.In): CreateProductCommand.Out {
        val response = productServiceStub.createProduct(
            CreateProductRequest.newBuilder()
                .setName(command.name)
                .setDescription(command.description)
                .setPrice(command.price)
                .build()
        )

        return CreateProductCommand.Out(
            id = response.id,
            name = response.name,
            description = response.description,
            price = response.price,
        )
    }
}
