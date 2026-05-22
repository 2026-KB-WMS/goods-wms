package com.kb.wmslab.goods_wms.business.application.inbound;

public class InboundCommand {
    public record CreateInbound(Long warehouseId, Long handlerId, String supplierName) {}
    public record AddLine(Long productId, int orderedQuantity) {}
    public record RecordInspection(Long lineId, int normalQuantity, int damagedQuantity, int pendingInspectionQuantity) {}
}
