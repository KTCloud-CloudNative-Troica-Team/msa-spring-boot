package dev.ktcloud.black.admin.api.gateway.product.api

import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.product.AdminProductApiGatewayRestController
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.product.request.CreateProductRequest
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.product.response.CreateProductResponse
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.product.response.FetchProductResponse
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.product.response.FetchProductsResponse
import dev.ktcloud.black.admin.api.gateway.application.product.dto.ProductDto
import dev.ktcloud.black.admin.api.gateway.application.product.port.inbound.CreateProductCommand
import dev.ktcloud.black.admin.api.gateway.application.product.port.inbound.FetchProductQuery
import dev.ktcloud.black.admin.api.gateway.application.product.port.inbound.FetchProductsQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/api/v1/products")
class AdminProductApiGatewayRestControllerAdapter(
    private val createProductCommand: CreateProductCommand,
    private val fetchProductQuery: FetchProductQuery,
    private val fetchProductsQuery: FetchProductsQuery,
) : AdminProductApiGatewayRestController {
    @Operation(summary = "상품 등록")
    @ApiResponse(responseCode = "201", description = "상품 등록 성공")
    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping
    override suspend fun createProduct(@RequestBody request: CreateProductRequest): CreateProductResponse {
        val result = createProductCommand.createProduct(
            CreateProductCommand.In(
                name = request.name,
                description = request.description,
                price = request.price,
            )
        )

        return CreateProductResponse(
            product = ProductDto(
                id = result.id,
                name = result.name,
                description = result.description,
                price = result.price,
            )
        )
    }

    @Operation(summary = "상품 단건 조회")
    @ApiResponse(responseCode = "200", description = "상품 조회 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping("{id}")
    override suspend fun fetchProduct(@PathVariable id: String): FetchProductResponse {
        val result = fetchProductQuery.fetchProduct(
            FetchProductQuery.In(id)
        )

        return FetchProductResponse(
            product = ProductDto(
                id = result.id,
                name = result.name,
                description = result.description,
                price = result.price,
            )
        )
    }

    @Operation(summary = "상품 전체 조회")
    @ApiResponse(responseCode = "200", description = "상품 전체 조회 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping
    override suspend fun fetchProducts(): FetchProductsResponse {
        val result = fetchProductsQuery.fetchProducts()

        return FetchProductsResponse(
            products = result.map {
                ProductDto(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    price = it.price,
                )
            }
        )
    }
}
