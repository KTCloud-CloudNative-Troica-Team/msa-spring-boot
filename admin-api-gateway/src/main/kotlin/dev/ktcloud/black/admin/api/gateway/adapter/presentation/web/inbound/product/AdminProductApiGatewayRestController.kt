package dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.product

import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.product.request.CreateProductRequest
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.product.response.CreateProductResponse
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.product.response.FetchProductResponse
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.product.response.FetchProductsResponse

interface AdminProductApiGatewayRestController {
    suspend fun createProduct(request: CreateProductRequest): CreateProductResponse
    suspend fun fetchProduct(id: String): FetchProductResponse
    suspend fun fetchProducts(): FetchProductsResponse
}
