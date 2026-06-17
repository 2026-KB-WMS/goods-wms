package com.kb.wmslab.goods_wms.business.application.inventory;

import com.kb.wmslab.goods_wms.business.domain.common.exception.InventoryNotFoundException;
import com.kb.wmslab.goods_wms.business.domain.inventory.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService implements InventoryUseCase {

    private final InventoryRepository inventoryRepository;

    @Override
    @Cacheable(value = "inventory", key = "#warehouseId + ':' + #zoneId + ':' + #productId")
    public InventoryResult getInventory(Long warehouseId, Long zoneId, Long productId) {
        return inventoryRepository
                .findByWarehouseIdAndZoneIdAndProductId(warehouseId, zoneId, productId)
                .map(InventoryResult::from)
                .orElseThrow(() -> new InventoryNotFoundException(warehouseId, zoneId, productId));
    }

    @Override
    @Cacheable(value = "inventoryByWarehouse", key = "#warehouseId")
    public List<InventoryResult> getInventoriesByWarehouse(Long warehouseId) {
        return inventoryRepository.findAllByWarehouseId(warehouseId).stream()
                .map(InventoryResult::from)
                .collect(Collectors.toList());
    }
}
