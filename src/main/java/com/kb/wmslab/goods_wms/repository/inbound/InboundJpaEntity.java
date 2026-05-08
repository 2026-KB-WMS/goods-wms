package com.kb.wmslab.goods_wms.repository.inbound;

import com.kb.wmslab.goods_wms.business.domain.inbound.Inbound;
import com.kb.wmslab.goods_wms.business.domain.inbound.InboundStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inbounds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboundJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long warehouseId;
    private Long handlerId;
    private String supplierName;

    @Enumerated(EnumType.STRING)
    private InboundStatus status;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "inbound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InboundLineJpaEntity> lines = new ArrayList<>();

    private InboundJpaEntity(Long warehouseId, Long handlerId, String supplierName,
                              InboundStatus status, LocalDateTime createdAt) {
        this.warehouseId = warehouseId;
        this.handlerId = handlerId;
        this.supplierName = supplierName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static InboundJpaEntity from(Inbound inbound) {
        InboundJpaEntity entity = new InboundJpaEntity(
                inbound.getWarehouseId(), inbound.getHandlerId(), inbound.getSupplierName(),
                inbound.getStatus(), inbound.getCreatedAt()
        );
        entity.id = inbound.getId();
        inbound.getLines().forEach(line ->
                entity.lines.add(InboundLineJpaEntity.from(line, entity))
        );
        return entity;
    }

    public Inbound toDomain() {
        return Inbound.reconstitute(
                id, warehouseId, handlerId, supplierName, status,
                lines.stream().map(InboundLineJpaEntity::toDomain).toList(),
                createdAt
        );
    }
}
