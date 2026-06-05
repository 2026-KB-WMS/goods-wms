package com.kb.wmslab.goods_wms.controller.warehouse;

import com.kb.wmslab.goods_wms.business.domain.warehouse.ZoneType;

public class WarehouseRequest {
    public record CreateWarehouseRequest(String name, String address) {}
    public record AddZoneRequest(String zoneCode, String zoneName, ZoneType zoneType) {}
}
