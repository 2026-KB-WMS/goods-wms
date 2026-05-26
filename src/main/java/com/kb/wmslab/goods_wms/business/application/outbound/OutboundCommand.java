package com.kb.wmslab.goods_wms.business.application.outbound;

import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundPurpose;

public class OutboundCommand {
    public record CreateOutbound(Long warehouseId, Long handlerId, String destination, OutboundPurpose purpose) {}
    public record AddLine(Long productId, int quantity) {}
}
