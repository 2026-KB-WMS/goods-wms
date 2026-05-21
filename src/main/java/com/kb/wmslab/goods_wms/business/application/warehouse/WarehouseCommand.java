package com.kb.wmslab.goods_wms.business.application.warehouse;

import com.kb.wmslab.goods_wms.business.domain.warehouse.ZoneType;

public class WarehouseCommand {
    public record CreateWarehouse(String name, String address) {}
    public record AddZone(String zoneCode, String zoneName, ZoneType zoneType) {}
}
