package com.kb.wmslab.goods_wms.batch.reconciliation;

import com.kb.wmslab.goods_wms.repository.inventory.InventoryJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 한 (warehouseId, zoneId, productId) 조합의 정합성을 검증한다.
 * - 입고 normal+pending 합계 - 출고 합계 → 기대 normal
 * - 입고 damaged 합계 → 기대 damaged
 * - inventory의 실제값과 비교하여 차이가 있으면 InventoryMismatchJpaEntity 반환
 * - 일치하면 null 반환 (Spring Batch가 자동으로 skip)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationProcessor
        implements ItemProcessor<InventoryJpaEntity, InventoryMismatchJpaEntity> {

    private final ReconciliationQueryRepository queryRepository;

    @Value("#{T(java.time.LocalDate).now()}")
    private LocalDate runDate;

    @Override
    public InventoryMismatchJpaEntity process(InventoryJpaEntity inventory) {
        Long warehouseId = inventory.getWarehouseId();
        Long zoneId      = inventory.getZoneId();
        Long productId   = inventory.getProductId();

        int inboundNormalPending = queryRepository.sumInboundNormalAndPending(warehouseId, zoneId, productId);
        int inboundDamaged       = queryRepository.sumInboundDamaged(warehouseId, zoneId, productId);
        int outbound             = queryRepository.sumOutbound(warehouseId, zoneId, productId);

        int expectedNormal  = inboundNormalPending - outbound;
        int actualNormal    = inventory.getNormalQuantity() + inventory.getPendingInspectionQuantity();
        int expectedDamaged = inboundDamaged;
        int actualDamaged   = inventory.getDamagedQuantity();

        if (expectedNormal == actualNormal && expectedDamaged == actualDamaged) {
            return null;
        }

        log.warn("[Reconciliation Mismatch] warehouse={} zone={} product={} " +
                        "normal(expected={}, actual={}, diff={}) damaged(expected={}, actual={}, diff={})",
                warehouseId, zoneId, productId,
                expectedNormal, actualNormal, actualNormal - expectedNormal,
                expectedDamaged, actualDamaged, actualDamaged - expectedDamaged);

        return InventoryMismatchJpaEntity.builder()
                .runDate(runDate)
                .warehouseId(warehouseId)
                .zoneId(zoneId)
                .productId(productId)
                .expectedNormal(expectedNormal)
                .actualNormal(actualNormal)
                .expectedDamaged(expectedDamaged)
                .actualDamaged(actualDamaged)
                .build();
    }
}
