package com.kb.wmslab.goods_wms.business.application.inbound;

import com.kb.wmslab.goods_wms.business.domain.inbound.Inbound;
import com.kb.wmslab.goods_wms.business.domain.inbound.InboundLine;
import com.kb.wmslab.goods_wms.business.domain.inbound.InboundStatus;

import java.time.LocalDateTime;
import java.util.List;

public record InboundResult(
        Long id, Long warehouseId, Long handlerId, String supplierName,
        InboundStatus status, List<LineResult> lines, LocalDateTime createdAt
) {
    public record LineResult(
            Long id, Long productId, int orderedQuantity,
            int normalQuantity, int damagedQuantity, int pendingInspectionQuantity, boolean inspected
    ) {
        public static LineResult from(InboundLine line) {
            return new LineResult(
                    line.getId(), line.getProductId(), line.getOrderedQuantity(),
                    line.getNormalQuantity(), line.getDamagedQuantity(),
                    line.getPendingInspectionQuantity(), line.isInspected()
            );
        }
    }

    public static InboundResult from(Inbound inbound) {
        return new InboundResult(
                inbound.getId(), inbound.getWarehouseId(), inbound.getHandlerId(),
                inbound.getSupplierName(), inbound.getStatus(),
                inbound.getLines().stream().map(LineResult::from).toList(),
                inbound.getCreatedAt()
        );
    }
}
