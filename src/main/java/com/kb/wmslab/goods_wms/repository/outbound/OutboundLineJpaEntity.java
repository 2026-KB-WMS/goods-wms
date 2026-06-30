package com.kb.wmslab.goods_wms.repository.outbound;

import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundLine;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbound_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboundLineJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_id", nullable = false)
    private OutboundJpaEntity outbound;

    private Long productId;
    private int quantity;
    private Long zoneId;

    private OutboundLineJpaEntity(OutboundJpaEntity outbound, Long productId, int quantity, Long zoneId) {
        this.outbound = outbound;
        this.productId = productId;
        this.quantity = quantity;
        this.zoneId = zoneId;
    }

    public static OutboundLineJpaEntity from(OutboundLine line, OutboundJpaEntity outbound) {
        OutboundLineJpaEntity entity = new OutboundLineJpaEntity(outbound, line.getProductId(), line.getQuantity(), line.getZoneId());
        entity.id = line.getId();
        entity.version = line.getVersion();
        return entity;
    }

    public OutboundLine toDomain() {
        return OutboundLine.reconstitute(id, productId, quantity, zoneId, version);
    }
}
