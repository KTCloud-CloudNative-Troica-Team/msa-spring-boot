package dev.ktcloud.black.inventory.application.service

import dev.ktcloud.black.client.redis.api.DistributedLock
import dev.ktcloud.black.inventory.application.port.inbound.query.FetchInventoriesQuery
import dev.ktcloud.black.inventory.application.port.inbound.query.LoadInventoryQuery
import dev.ktcloud.black.inventory.application.port.outbound.LoadCacheSyncedInventoryOutboundPort
import dev.ktcloud.black.inventory.application.port.state.outbound.InventoryStateQueryOutboundPort
import dev.ktcloud.black.inventory.domain.exception.InventoryException
import dev.ktcloud.black.inventory.domain.vo.InventoryLockKey
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

@Service
class InventoryQueryService(
    private val distributedLock: DistributedLock,
    private val loadCacheSyncedInventoryOutboundPort: LoadCacheSyncedInventoryOutboundPort,
    private val inventoryStateQueryOutboundPort: InventoryStateQueryOutboundPort
): LoadInventoryQuery, FetchInventoriesQuery {
    @Transactional(readOnly = true)
    override fun load(query: LoadInventoryQuery.In): LoadInventoryQuery.Out {
        val inventory = try {
            loadCacheSyncedInventoryOutboundPort.loadInventory(query.id)
        } catch (_: InventoryException.NoCachedInventoryFound) {
            distributedLock.execute(
                key = InventoryLockKey(query.id).toLockKey(),
                func = {
                    loadCacheSyncedInventoryOutboundPort.loadCacheSyncedInventory(query.id)
                }
            )
        }

        return LoadInventoryQuery.Out(
            id = inventory.id,
            productId = inventory.productId,
            skuCode = inventory.skuCode,
            quantity = inventory.quantity
        )
    }

    @Transactional(readOnly = true)
    override fun fetchAll(): List<FetchInventoriesQuery.Out> {
        val all = inventoryStateQueryOutboundPort.fetchAll()

        val loadedAll = runBlocking {
            all.map { inventory ->
                async(Dispatchers.IO) {
                    try {
                        loadCacheSyncedInventoryOutboundPort.loadInventory(inventory.id)
                    } catch (_: InventoryException.NoCachedInventoryFound) {
                        distributedLock.execute(
                            key = InventoryLockKey(inventory.id).toLockKey(),
                            func = {
                                loadCacheSyncedInventoryOutboundPort.loadCacheSyncedInventory(inventory.id)
                            }
                        )
                    }
                }
            }.awaitAll()
        }

        return loadedAll.map {
            FetchInventoriesQuery.Out(
                id = it.id,
                productId = it.productId,
                skuCode = it.skuCode,
                quantity = it.quantity
            )
        }
    }
}