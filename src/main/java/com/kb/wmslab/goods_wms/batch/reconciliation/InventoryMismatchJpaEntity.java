package com.kb.wmslab.goods_wms.batch.reconciliation;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_mismatch",
        indexes = {
                @Index(name = "idx_mismatch_run_date", columnList = "runDate"),
                @Index(name = "idx_mismatch_key", columnList = "warehouseId, zoneId, productId")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryMismatchJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate runDate;

    private Long warehouseId;
    private Long zoneId;
    private Long productId;

    private int expectedNormal;
    private int actualNormal;
    private int diffNormal;

    private int expectedDamaged;
    private int actualDamaged;
    private int diffDamaged;

    private LocalDateTime createdAt;

    @Builder
    private InventoryMismatchJpaEntity(LocalDate runDate, Long warehouseId, Long zoneId, Long productId,
                                       int expectedNormal, int actualNormal,
                                       int expectedDamaged, int actualDamaged) {
        this.runDate = runDate;
        this.warehouseId = warehouseId;
        this.zoneId = zoneId;
        this.productId = productId;
        this.expectedNormal = expectedNormal;
        this.actualNormal = actualNormal;
        this.diffNormal = actualNormal - expectedNormal;
        this.expectedDamaged = expectedDamaged;
        this.actualDamaged = actualDamaged;
        this.diffDamaged = actualDamaged - expectedDamaged;
        this.createdAt = LocalDateTime.now();
    }
}
