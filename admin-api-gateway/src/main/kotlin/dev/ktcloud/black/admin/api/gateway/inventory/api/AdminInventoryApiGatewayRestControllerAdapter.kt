package dev.ktcloud.black.admin.api.gateway.inventory.api

import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.AdminInventoryApiGatewayRestController
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.request.AdjustInventoryRequest
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.request.CreateInventoryRequest
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.request.DecreaseInventoryRequest
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.response.AdjustInventoryResponse
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.response.CreateInventoryResponse
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.response.FetchInventoriesResponse
import dev.ktcloud.black.admin.api.gateway.adapter.presentation.web.inbound.inventory.response.FetchInventoryResponse
import dev.ktcloud.black.admin.api.gateway.application.inventory.dto.InventoryDto
import dev.ktcloud.black.admin.api.gateway.application.inventory.port.inbound.CreateInventoryCommand
import dev.ktcloud.black.admin.api.gateway.application.inventory.port.inbound.DecreaseInventoryCommand
import dev.ktcloud.black.admin.api.gateway.application.inventory.port.inbound.FetchInventoriesQuery
import dev.ktcloud.black.admin.api.gateway.application.inventory.port.inbound.FetchInventoryQuery
import dev.ktcloud.black.admin.api.gateway.application.inventory.port.inbound.IncreaseInventoryCommand
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
@RequestMapping("/admin/api/v1/inventories")
class AdminInventoryApiGatewayRestControllerAdapter(
    private val createInventoryCommand: CreateInventoryCommand,
    private val increaseInventoryCommand: IncreaseInventoryCommand,
    private val decreaseInventoryCommand: DecreaseInventoryCommand,
    private val fetchInventoryQuery: FetchInventoryQuery,
    private val fetchInventoriesQuery: FetchInventoriesQuery,
) : AdminInventoryApiGatewayRestController {
    @Operation(summary = "재고 등록")
    @ApiResponse(responseCode = "201", description = "재고 등록 성공")
    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping
    override suspend fun createInventory(@RequestBody request: CreateInventoryRequest): CreateInventoryResponse {
        val result = createInventoryCommand.createInventory(
            CreateInventoryCommand.In(
                productId = request.productId,
                skuCode = request.skuCode,
            )
        )

        return CreateInventoryResponse(
            inventory = InventoryDto(
                id = result.id,
                productId = result.productId,
                skuCode = result.skuCode,
                quantity = result.quantity,
            )
        )
    }

    @Operation(summary = "재고 입고 (수량 증가)")
    @ApiResponse(responseCode = "200", description = "재고 증가 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @PostMapping("{id}/increase")
    override suspend fun increaseInventory(
        @PathVariable id: Long,
        @RequestBody request: AdjustInventoryRequest,
    ): AdjustInventoryResponse {
        val result = increaseInventoryCommand.increaseInventory(
            IncreaseInventoryCommand.In(
                inventoryId = id,
                amount = request.amount,
            )
        )

        return AdjustInventoryResponse(
            inventory = InventoryDto(
                id = result.id,
                productId = result.productId,
                skuCode = result.skuCode,
                quantity = result.quantity,
            )
        )
    }

    @Operation(summary = "재고 출고 (수량 감소)")
    @ApiResponse(responseCode = "200", description = "재고 감소 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @PostMapping("{id}/decrease")
    override suspend fun decreaseInventory(
        @PathVariable id: Long,
        @RequestBody request: DecreaseInventoryRequest,
    ): AdjustInventoryResponse {
        val result = decreaseInventoryCommand.decreaseInventory(
            DecreaseInventoryCommand.In(
                orderId = request.orderId,
                inventoryId = id,
                amount = request.amount,
            )
        )

        return AdjustInventoryResponse(
            inventory = InventoryDto(
                id = result.id,
                productId = result.productId,
                skuCode = result.skuCode,
                quantity = result.quantity,
            )
        )
    }

    @Operation(summary = "재고 단건 조회")
    @ApiResponse(responseCode = "200", description = "재고 조회 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping("{id}")
    override suspend fun fetchInventory(@PathVariable id: Long): FetchInventoryResponse {
        val result = fetchInventoryQuery.fetchInventory(
            FetchInventoryQuery.In(id)
        )

        return FetchInventoryResponse(
            inventory = InventoryDto(
                id = result.id,
                productId = result.productId,
                skuCode = result.skuCode,
                quantity = result.quantity,
            )
        )
    }

    @Operation(summary = "재고 전체 조회")
    @ApiResponse(responseCode = "200", description = "재고 전체 조회 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping
    override suspend fun fetchInventories(): FetchInventoriesResponse {
        val result = fetchInventoriesQuery.fetchAll()

        return FetchInventoriesResponse(
            inventories = result.map {
                InventoryDto(
                    id = it.id,
                    productId = it.productId,
                    skuCode = it.skuCode,
                    quantity = it.quantity,
                )
            }
        )
    }
}
