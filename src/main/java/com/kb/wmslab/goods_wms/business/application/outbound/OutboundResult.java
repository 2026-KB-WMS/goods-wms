package com.kb.wmslab.goods_wms.business.application.outbound;

import com.kb.wmslab.goods_wms.business.domain.outbound.Outbound;
import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundLine;
import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundPurpose;
import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OutboundResult(
        Long id, Long warehouseId, Long handlerId, String destination,
        OutboundPurpose purpose, OutboundStatus status, List<LineResult> lines, LocalDateTime createdAt
) {
    public record LineResult(Long id, Long productId, int quantity, Long zoneId) {
        public static LineResult from(OutboundLine line) {
            return new LineResult(line.getId(), line.getProductId(), line.getQuantity(), line.getZoneId());
        }
    }

    public static OutboundResult from(Outbound outbound) {
        return new OutboundResult(
                outbound.getId(), outbound.getWarehouseId(), outbound.getHandlerId(),
                outbound.getDestination(), outbound.getPurpose(), outbound.getStatus(),
                outbound.getLines().stream().map(LineResult::from).toList(),
                outbound.getCreatedAt()
        );
    }
}
