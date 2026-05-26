package com.kb.wmslab.goods_wms.business.application.outbound;

public interface OutboundUseCase {
    OutboundResult createOutbound(OutboundCommand.CreateOutbound command);
    OutboundResult addLine(Long outboundId, OutboundCommand.AddLine command);
    OutboundResult validateOutbound(Long outboundId);
    OutboundResult completeOutbound(Long outboundId);
    OutboundResult cancelOutbound(Long outboundId);
    OutboundResult getOutbound(Long id);
}
