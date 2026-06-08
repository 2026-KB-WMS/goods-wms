package com.kb.wmslab.goods_wms.business.domain.inbound;

import com.kb.wmslab.goods_wms.business.domain.common.exception.InvalidStatusTransitionException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class Inbound {

    private final Long id;
    private final Long warehouseId;
    private final Long handlerId;
    private String supplierName;
    private InboundStatus status;
    private final List<InboundLine> lines;
    private final LocalDateTime createdAt;
    private final Long version;

    private Inbound(Long id, Long warehouseId, Long handlerId, String supplierName,
                    InboundStatus status, List<InboundLine> lines, LocalDateTime createdAt, Long version) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.handlerId = handlerId;
        this.supplierName = supplierName;
        this.status = status;
        this.lines = new ArrayList<>(lines);
        this.createdAt = createdAt;
        this.version = version;
    }

    public static Inbound create(Long warehouseId, Long handlerId, String supplierName) {
        return new Inbound(null, warehouseId, handlerId, supplierName,
                InboundStatus.CREATED, List.of(), LocalDateTime.now(), null);
    }

    public static Inbound reconstitute(Long id, Long warehouseId, Long handlerId, String supplierName,
                                        InboundStatus status, List<InboundLine> lines,
                                        LocalDateTime createdAt, Long version) {
        return new Inbound(id, warehouseId, handlerId, supplierName, status, lines, createdAt, version);
    }

    public void addLine(InboundLine line) {
        if (status != InboundStatus.CREATED) {
            throw new IllegalStateException("CREATED 상태에서만 입고 라인을 추가할 수 있습니다.");
        }
        lines.add(line);
    }

    public void startInspection() {
        if (status != InboundStatus.CREATED) {
            throw new InvalidStatusTransitionException(status.name(), InboundStatus.INSPECTING.name());
        }
        if (lines.isEmpty()) {
            throw new IllegalStateException("입고 라인이 없으면 검수를 시작할 수 없습니다.");
        }
        this.status = InboundStatus.INSPECTING;
    }

    public void complete() {
        if (status != InboundStatus.INSPECTING) {
            throw new InvalidStatusTransitionException(status.name(), InboundStatus.COMPLETED.name());
        }
        boolean allInspected = lines.stream().allMatch(InboundLine::isInspected);
        if (!allInspected) {
            throw new IllegalStateException("모든 입고 라인의 검수가 완료되어야 입고를 완료할 수 있습니다.");
        }
        this.status = InboundStatus.COMPLETED;
    }

    public void cancel() {
        if (status == InboundStatus.COMPLETED) {
            throw new InvalidStatusTransitionException(status.name(), InboundStatus.CANCELED.name());
        }
        this.status = InboundStatus.CANCELED;
    }

    public List<InboundLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
