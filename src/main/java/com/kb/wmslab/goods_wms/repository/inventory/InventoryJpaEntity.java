package com.kb.wmslab.goods_wms.repository.inventory;

import com.kb.wmslab.goods_wms.business.domain.inventory.Inventory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"warehouseId", "zoneId", "productId"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long warehouseId;
    private Long zoneId;
    private Long productId;
    private int normalQuantity;
    private int damagedQuantity;
    private int pendingInspectionQuantity;
    private int reservedQuantity;

    private InventoryJpaEntity(Long warehouseId, Long zoneId, Long productId,
                                int normalQuantity, int damagedQuantity,
                                int pendingInspectionQuantity, int reservedQuantity) {
        this.warehouseId = warehouseId;
        this.zoneId = zoneId;
        this.productId = productId;
        this.normalQuantity = normalQuantity;
        this.damagedQuantity = damagedQuantity;
        this.pendingInspectionQuantity = pendingInspectionQuantity;
        this.reservedQuantity = reservedQuantity;
    }

    public static InventoryJpaEntity from(Inventory inventory) {
        InventoryJpaEntity entity = new InventoryJpaEntity(
                inventory.getWarehouseId(), inventory.getZoneId(), inventory.getProductId(),
                inventory.getNormalQuantity(), inventory.getDamagedQuantity(),
                inventory.getPendingInspectionQuantity(), inventory.getReservedQuantity()
        );
        entity.id = inventory.getId();
        return entity;
    }

    public Inventory toDomain() {
        return Inventory.reconstitute(id, warehouseId, zoneId, productId,
                normalQuantity, damagedQuantity, pendingInspectionQuantity, reservedQuantity);
    }
}
