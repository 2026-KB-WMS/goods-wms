package com.kb.wmslab.goods_wms.business.application.warehouse;

import com.kb.wmslab.goods_wms.business.domain.warehouse.Warehouse;
import com.kb.wmslab.goods_wms.business.domain.warehouse.WarehouseStatus;
import com.kb.wmslab.goods_wms.business.domain.warehouse.WarehouseZone;
import com.kb.wmslab.goods_wms.business.domain.warehouse.ZoneType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record WarehouseResult(
        Long id, String name, String address, WarehouseStatus status,
        List<ZoneResult> zones, LocalDateTime createdAt
) {
    public record ZoneResult(Long id, String zoneCode, String zoneName, ZoneType zoneType) {
        public static ZoneResult from(WarehouseZone zone) {
            return new ZoneResult(zone.getId(), zone.getZoneCode(), zone.getZoneName(), zone.getZoneType());
        }
    }

    public static WarehouseResult from(Warehouse warehouse) {
        return new WarehouseResult(
                warehouse.getId(), warehouse.getName(), warehouse.getAddress(), warehouse.getStatus(),
                warehouse.getZones().stream().map(ZoneResult::from).collect(Collectors.toList()),
                warehouse.getCreatedAt()
        );
    }
}
