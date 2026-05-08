package com.kb.wmslab.goods_wms.repository.warehouse;

import com.kb.wmslab.goods_wms.business.domain.warehouse.Warehouse;
import com.kb.wmslab.goods_wms.business.domain.warehouse.WarehouseStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warehouses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarehouseJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;

    @Enumerated(EnumType.STRING)
    private WarehouseStatus status;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WarehouseZoneJpaEntity> zones = new ArrayList<>();

    private WarehouseJpaEntity(String name, String address, WarehouseStatus status, LocalDateTime createdAt) {
        this.name = name;
        this.address = address;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static WarehouseJpaEntity from(Warehouse warehouse) {
        WarehouseJpaEntity entity = new WarehouseJpaEntity(
                warehouse.getName(), warehouse.getAddress(),
                warehouse.getStatus(), warehouse.getCreatedAt()
        );
        entity.id = warehouse.getId();
        warehouse.getZones().forEach(zone ->
                entity.zones.add(WarehouseZoneJpaEntity.from(zone, entity))
        );
        return entity;
    }

    public Warehouse toDomain() {
        return Warehouse.reconstitute(
                id, name, address, status,
                zones.stream().map(WarehouseZoneJpaEntity::toDomain).toList(),
                createdAt
        );
    }
}
