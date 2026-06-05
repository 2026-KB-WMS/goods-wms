package com.kb.wmslab.goods_wms.business.domain.warehouse;

import com.kb.wmslab.goods_wms.business.domain.common.exception.InactiveEntityException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class Warehouse {

    private final Long id;
    private String name;
    private String address;
    private WarehouseStatus status;
    private final List<WarehouseZone> zones;
    private final LocalDateTime createdAt;
    private final Long version;

    private Warehouse(Long id, String name, String address, WarehouseStatus status,
                      List<WarehouseZone> zones, LocalDateTime createdAt, Long version) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.status = status;
        this.zones = new ArrayList<>(zones);
        this.createdAt = createdAt;
        this.version = version;
    }

    public static Warehouse create(String name, String address) {
        return new Warehouse(null, name, address, WarehouseStatus.ACTIVE, List.of(), LocalDateTime.now(), null);
    }

    public static Warehouse reconstitute(Long id, String name, String address, WarehouseStatus status,
                                          List<WarehouseZone> zones, LocalDateTime createdAt, Long version) {
        return new Warehouse(id, name, address, status, zones, createdAt, version);
    }

    public void activate() {
        this.status = WarehouseStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = WarehouseStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == WarehouseStatus.ACTIVE;
    }

    public void addZone(WarehouseZone zone) {
        boolean duplicated = zones.stream().anyMatch(z -> z.hasSameCode(zone.getZoneCode()));
        if (duplicated) {
            throw new IllegalArgumentException("동일한 구역 코드가 이미 존재합니다: " + zone.getZoneCode());
        }
        zones.add(zone);
    }

    public List<WarehouseZone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    public void validateAvailableForTransaction() {
        if (!isActive()) throw new InactiveEntityException("Warehouse", id);
    }
}
