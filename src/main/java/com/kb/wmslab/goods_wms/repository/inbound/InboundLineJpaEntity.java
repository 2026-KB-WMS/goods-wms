package com.kb.wmslab.goods_wms.repository.inbound;

import com.kb.wmslab.goods_wms.business.domain.inbound.InboundLine;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inbound_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboundLineJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inbound_id", nullable = false)
    private InboundJpaEntity inbound;

    private Long productId;
    private int orderedQuantity;
    private int normalQuantity;
    private int damagedQuantity;
    private int pendingInspectionQuantity;
    private boolean inspected;

    private InboundLineJpaEntity(InboundJpaEntity inbound, Long productId, int orderedQuantity,
                                  int normalQuantity, int damagedQuantity,
                                  int pendingInspectionQuantity, boolean inspected) {
        this.inbound = inbound;
        this.productId = productId;
        this.orderedQuantity = orderedQuantity;
        this.normalQuantity = normalQuantity;
        this.damagedQuantity = damagedQuantity;
        this.pendingInspectionQuantity = pendingInspectionQuantity;
        this.inspected = inspected;
    }

    public static InboundLineJpaEntity from(InboundLine line, InboundJpaEntity inbound) {
        InboundLineJpaEntity entity = new InboundLineJpaEntity(
                inbound, line.getProductId(), line.getOrderedQuantity(),
                line.getNormalQuantity(), line.getDamagedQuantity(),
                line.getPendingInspectionQuantity(), line.isInspected()
        );
        entity.id = line.getId();
        entity.version = line.getVersion();
        return entity;
    }

    public InboundLine toDomain() {
        return InboundLine.reconstitute(id, productId, orderedQuantity,
                normalQuantity, damagedQuantity, pendingInspectionQuantity, inspected, version);
    }
}
