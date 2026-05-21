package com.kb.wmslab.goods_wms.business.application.warehouse;

public interface WarehouseUseCase {
    WarehouseResult createWarehouse(WarehouseCommand.CreateWarehouse command);
    WarehouseResult addZone(Long warehouseId, WarehouseCommand.AddZone command);
    WarehouseResult activateWarehouse(Long id);
    WarehouseResult deactivateWarehouse(Long id);
    WarehouseResult getWarehouse(Long id);
}
