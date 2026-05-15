package dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory

import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.request.AdjustInventoryRequest
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.request.CreateInventoryRequest
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.request.DecreaseInventoryRequest
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.response.AdjustInventoryResponse
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.response.CreateInventoryResponse
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.response.FetchInventoriesResponse
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.response.FetchInventoryResponse

interface AdminInventoryApiGatewayRestController {
    suspend fun createInventory(request: CreateInventoryRequest): CreateInventoryResponse
    suspend fun increaseInventory(id: Long, request: AdjustInventoryRequest): AdjustInventoryResponse
    suspend fun decreaseInventory(id: Long, request: DecreaseInventoryRequest): AdjustInventoryResponse
    suspend fun fetchInventory(id: Long): FetchInventoryResponse
    suspend fun fetchInventories(): FetchInventoriesResponse
}
