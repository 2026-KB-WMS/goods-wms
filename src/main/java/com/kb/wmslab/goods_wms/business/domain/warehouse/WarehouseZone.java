package com.kb.wmslab.goods_wms.business.domain.warehouse;

import lombok.Getter;

@Getter
public class WarehouseZone {

    private final Long id;
    private final String zoneCode;
    private String zoneName;
    private ZoneType zoneType;
    private final Long version;

    private WarehouseZone(Long id, String zoneCode, String zoneName, ZoneType zoneType, Long version) {
        this.id = id;
        this.zoneCode = zoneCode;
        this.zoneName = zoneName;
        this.zoneType = zoneType;
        this.version = version;
    }

    public static WarehouseZone create(String zoneCode, String zoneName, ZoneType zoneType) {
        return new WarehouseZone(null, zoneCode, zoneName, zoneType, null);
    }

    public static WarehouseZone reconstitute(Long id, String zoneCode, String zoneName,
                                              ZoneType zoneType, Long version) {
        return new WarehouseZone(id, zoneCode, zoneName, zoneType, version);
    }

    boolean hasSameCode(String code) {
        return this.zoneCode.equals(code);
    }
}
