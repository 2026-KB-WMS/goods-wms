package com.kb.wmslab.goods_wms.business.application.inventory;

import java.util.List;

public interface InventoryUseCase {
    InventoryResult getInventory(Long warehouseId, Long zoneId, Long productId);
    List<InventoryResult> getInventoriesByWarehouse(Long warehouseId);
}
