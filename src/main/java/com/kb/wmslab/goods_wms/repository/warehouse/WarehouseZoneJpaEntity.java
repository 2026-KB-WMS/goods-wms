package com.kb.wmslab.goods_wms.repository.warehouse;

import com.kb.wmslab.goods_wms.business.domain.warehouse.WarehouseZone;
import com.kb.wmslab.goods_wms.business.domain.warehouse.ZoneType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "warehouse_zones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarehouseZoneJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private WarehouseJpaEntity warehouse;

    @Column(nullable = false)
    private String zoneCode;

    private String zoneName;

    @Enumerated(EnumType.STRING)
    private ZoneType zoneType;

    private WarehouseZoneJpaEntity(WarehouseJpaEntity warehouse, String zoneCode, String zoneName, ZoneType zoneType) {
        this.warehouse = warehouse;
        this.zoneCode = zoneCode;
        this.zoneName = zoneName;
        this.zoneType = zoneType;
    }

    public static WarehouseZoneJpaEntity from(WarehouseZone zone, WarehouseJpaEntity warehouse) {
        WarehouseZoneJpaEntity entity = new WarehouseZoneJpaEntity(
                warehouse, zone.getZoneCode(), zone.getZoneName(), zone.getZoneType()
        );
        entity.id = zone.getId();
        return entity;
    }

    public WarehouseZone toDomain() {
        return WarehouseZone.reconstitute(id, zoneCode, zoneName, zoneType);
    }
}
