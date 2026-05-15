package dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.product.response

import dev.ktcloud.black.admin.api.gateway.application.product.dto.ProductDto

data class FetchProductsResponse(
    val products: List<ProductDto>
)
