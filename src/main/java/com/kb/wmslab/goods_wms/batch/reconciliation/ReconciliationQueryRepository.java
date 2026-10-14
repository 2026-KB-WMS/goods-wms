package com.kb.wmslab.goods_wms.batch.reconciliation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 재고 정합성 검증을 위한 입출고 이력 합산 쿼리.
 *
 * COMPLETED 상태의 입출고만 합산한다.
 *   - 입고 normal + pendingInspection → normal_zone_id로 적치
 *   - 입고 damaged → damaged_zone_id로 적치
 *   - 출고 → zone_id (NORMAL zone)에서 차감
 */
@Repository
@RequiredArgsConstructor
public class ReconciliationQueryRepository {

    @PersistenceContext
    private final EntityManager em;

    /**
     * 지정 zone에 적치된 입고 normal + pendingInspection 합계.
     */
    public int sumInboundNormalAndPending(Long warehouseId, Long zoneId, Long productId) {
        Long result = em.createQuery(
                "SELECT COALESCE(SUM(il.normalQuantity + il.pendingInspectionQuantity), 0L) " +
                        "FROM InboundLineJpaEntity il " +
                        "WHERE il.inbound.warehouseId = :warehouseId " +
                        "  AND il.normalZoneId        = :zoneId " +
                        "  AND il.productId           = :productId " +
                        "  AND il.inbound.status      = com.kb.wmslab.goods_wms.business.domain.inbound.InboundStatus.COMPLETED",
                Long.class)
                .setParameter("warehouseId", warehouseId)
                .setParameter("zoneId", zoneId)
                .setParameter("productId", productId)
                .getSingleResult();
        return result.intValue();
    }

    /**
     * 지정 zone에 적치된 입고 damaged 합계.
     */
    public int sumInboundDamaged(Long warehouseId, Long zoneId, Long productId) {
        Long result = em.createQuery(
                "SELECT COALESCE(SUM(il.damagedQuantity), 0L) " +
                        "FROM InboundLineJpaEntity il " +
                        "WHERE il.inbound.warehouseId = :warehouseId " +
                        "  AND il.damagedZoneId       = :zoneId " +
                        "  AND il.productId           = :productId " +
                        "  AND il.inbound.status      = com.kb.wmslab.goods_wms.business.domain.inbound.InboundStatus.COMPLETED",
                Long.class)
                .setParameter("warehouseId", warehouseId)
                .setParameter("zoneId", zoneId)
                .setParameter("productId", productId)
                .getSingleResult();
        return result.intValue();
    }

    /**
     * 지정 zone에서 출고된 합계 (COMPLETED).
     */
    public int sumOutbound(Long warehouseId, Long zoneId, Long productId) {
        Long result = em.createQuery(
                "SELECT COALESCE(SUM(ol.quantity), 0L) " +
                        "FROM OutboundLineJpaEntity ol " +
                        "WHERE ol.outbound.warehouseId = :warehouseId " +
                        "  AND ol.zoneId               = :zoneId " +
                        "  AND ol.productId            = :productId " +
                        "  AND ol.outbound.status      = com.kb.wmslab.goods_wms.business.domain.outbound.OutboundStatus.COMPLETED",
                Long.class)
                .setParameter("warehouseId", warehouseId)
                .setParameter("zoneId", zoneId)
                .setParameter("productId", productId)
                .getSingleResult();
        return result.intValue();
    }
}
