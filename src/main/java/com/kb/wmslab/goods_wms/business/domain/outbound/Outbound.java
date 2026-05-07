package com.kb.wmslab.goods_wms.business.domain.outbound;

import com.kb.wmslab.goods_wms.business.domain.common.exception.InvalidStatusTransitionException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class Outbound {

    private final Long id;
    private final Long warehouseId;
    private final Long handlerId;
    private String destination;
    private OutboundPurpose purpose;
    private OutboundStatus status;
    private final List<OutboundLine> lines;
    private final LocalDateTime createdAt;

    private Outbound(Long id, Long warehouseId, Long handlerId, String destination,
                     OutboundPurpose purpose, OutboundStatus status,
                     List<OutboundLine> lines, LocalDateTime createdAt) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.handlerId = handlerId;
        this.destination = destination;
        this.purpose = purpose;
        this.status = status;
        this.lines = new ArrayList<>(lines);
        this.createdAt = createdAt;
    }

    public static Outbound create(Long warehouseId, Long handlerId, String destination, OutboundPurpose purpose) {
        return new Outbound(null, warehouseId, handlerId, destination, purpose,
                OutboundStatus.CREATED, List.of(), LocalDateTime.now());
    }

    public static Outbound reconstitute(Long id, Long warehouseId, Long handlerId, String destination,
                                         OutboundPurpose purpose, OutboundStatus status,
                                         List<OutboundLine> lines, LocalDateTime createdAt) {
        return new Outbound(id, warehouseId, handlerId, destination, purpose, status, lines, createdAt);
    }

    public void addLine(OutboundLine line) {
        if (status != OutboundStatus.CREATED) {
            throw new IllegalStateException("CREATED 상태에서만 출고 라인을 추가할 수 있습니다.");
        }
        lines.add(line);
    }

    public void validate() {
        if (status != OutboundStatus.CREATED) {
            throw new InvalidStatusTransitionException(status.name(), OutboundStatus.VALIDATED.name());
        }
        if (lines.isEmpty()) {
            throw new IllegalStateException("출고 라인이 없으면 출고 요청을 검증할 수 없습니다.");
        }
        this.status = OutboundStatus.VALIDATED;
    }

    public void complete() {
        if (status != OutboundStatus.VALIDATED) {
            throw new InvalidStatusTransitionException(status.name(), OutboundStatus.COMPLETED.name());
        }
        this.status = OutboundStatus.COMPLETED;
    }

    public void cancel() {
        if (status == OutboundStatus.COMPLETED) {
            throw new InvalidStatusTransitionException(status.name(), OutboundStatus.CANCELED.name());
        }
        this.status = OutboundStatus.CANCELED;
    }

    public List<OutboundLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
