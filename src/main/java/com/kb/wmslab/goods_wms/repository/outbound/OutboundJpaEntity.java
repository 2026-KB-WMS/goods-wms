package com.kb.wmslab.goods_wms.repository.outbound;

import com.kb.wmslab.goods_wms.business.domain.outbound.Outbound;
import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundPurpose;
import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "outbounds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboundJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long warehouseId;
    private Long handlerId;
    private String destination;

    @Enumerated(EnumType.STRING)
    private OutboundPurpose purpose;

    @Enumerated(EnumType.STRING)
    private OutboundStatus status;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "outbound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OutboundLineJpaEntity> lines = new ArrayList<>();

    private OutboundJpaEntity(Long warehouseId, Long handlerId, String destination,
                               OutboundPurpose purpose, OutboundStatus status, LocalDateTime createdAt) {
        this.warehouseId = warehouseId;
        this.handlerId = handlerId;
        this.destination = destination;
        this.purpose = purpose;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static OutboundJpaEntity from(Outbound outbound) {
        OutboundJpaEntity entity = new OutboundJpaEntity(
                outbound.getWarehouseId(), outbound.getHandlerId(), outbound.getDestination(),
                outbound.getPurpose(), outbound.getStatus(), outbound.getCreatedAt()
        );
        entity.id = outbound.getId();
        outbound.getLines().forEach(line ->
                entity.lines.add(OutboundLineJpaEntity.from(line, entity))
        );
        return entity;
    }

    public Outbound toDomain() {
        return Outbound.reconstitute(
                id, warehouseId, handlerId, destination, purpose, status,
                lines.stream().map(OutboundLineJpaEntity::toDomain).toList(),
                createdAt
        );
    }
}
