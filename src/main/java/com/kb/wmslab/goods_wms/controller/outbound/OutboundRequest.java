package com.kb.wmslab.goods_wms.controller.outbound;

import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundPurpose;

public class OutboundRequest {
    public record CreateOutboundRequest(Long warehouseId, Long handlerId, String destination, OutboundPurpose purpose) {}
    public record AddLineRequest(Long productId, int quantity) {}
}
