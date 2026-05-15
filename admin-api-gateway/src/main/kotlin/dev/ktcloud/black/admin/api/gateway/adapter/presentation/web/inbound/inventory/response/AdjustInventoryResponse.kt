package dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.response

import dev.ktcloud.black.admin.api.gateway.application.inventory.dto.InventoryDto

data class AdjustInventoryResponse(
    val inventory: InventoryDto
)
