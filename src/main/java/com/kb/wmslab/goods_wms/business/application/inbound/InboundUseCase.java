package com.kb.wmslab.goods_wms.business.application.inbound;

public interface InboundUseCase {
    InboundResult createInbound(InboundCommand.CreateInbound command);
    InboundResult addLine(Long inboundId, InboundCommand.AddLine command);
    InboundResult startInspection(Long inboundId);
    InboundResult recordLineInspection(Long inboundId, InboundCommand.RecordInspection command);
    InboundResult completeInbound(Long inboundId);
    InboundResult cancelInbound(Long inboundId);
    InboundResult getInbound(Long id);
}
