package com.kb.wmslab.goods_wms.business.application.inventory;

import com.kb.wmslab.goods_wms.business.domain.inventory.Inventory;

public record InventoryResult(
        Long id, Long warehouseId, Long zoneId, Long productId,
        int normalQuantity, int damagedQuantity, int pendingInspectionQuantity,
        int reservedQuantity, int availableQuantity
) {
    public static InventoryResult from(Inventory inventory) {
        return new InventoryResult(
                inventory.getId(), inventory.getWarehouseId(), inventory.getZoneId(),
                inventory.getProductId(), inventory.getNormalQuantity(), inventory.getDamagedQuantity(),
                inventory.getPendingInspectionQuantity(), inventory.getReservedQuantity(),
                inventory.getAvailableQuantity()
        );
    }
}
