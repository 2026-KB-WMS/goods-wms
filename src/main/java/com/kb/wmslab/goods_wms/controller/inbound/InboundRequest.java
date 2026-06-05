package com.kb.wmslab.goods_wms.controller.inbound;

public class InboundRequest {
    public record CreateInboundRequest(Long warehouseId, Long handlerId, String supplierName) {}
    public record AddLineRequest(Long productId, int orderedQuantity) {}
    public record RecordInspectionRequest(int normalQuantity, int damagedQuantity, int pendingInspectionQuantity) {}
}
