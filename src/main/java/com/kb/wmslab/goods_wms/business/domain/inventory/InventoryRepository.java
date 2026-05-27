package com.kb.wmslab.goods_wms.business.domain.inventory;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    Inventory save(Inventory inventory);
    Optional<Inventory> findByWarehouseIdAndZoneIdAndProductId(Long warehouseId, Long zoneId, Long productId);
    List<Inventory> findAllByWarehouseId(Long warehouseId);
}
