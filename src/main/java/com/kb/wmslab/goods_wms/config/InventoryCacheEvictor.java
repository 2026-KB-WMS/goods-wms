package com.kb.wmslab.goods_wms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryCacheEvictor {

    private final CacheManager cacheManager;

    public void evictInventoryEntry(Long warehouseId, Long zoneId, Long productId) {
        String key = warehouseId + ":" + zoneId + ":" + productId;
        afterCommit(() -> {
            getCache("inventory").evict(key);
            log.info("[CacheEvict] inventory :: {}", key);
        });
    }

    public void evictInventoryByWarehouse(Long warehouseId) {
        afterCommit(() -> {
            getCache("inventoryByWarehouse").evict(String.valueOf(warehouseId));
            log.info("[CacheEvict] inventoryByWarehouse :: {}", warehouseId);
        });
    }

    private void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private Cache getCache(String name) {
        Cache cache = cacheManager.getCache(name);
        if (cache == null) {
            throw new IllegalStateException("Cache not found: " + name);
        }
        return cache;
    }
}
