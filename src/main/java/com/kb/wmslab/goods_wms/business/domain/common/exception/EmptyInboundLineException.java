package com.kb.wmslab.goods_wms.business.domain.common.exception;

import com.kb.wmslab.goods_wms.controller.common.ErrorCode;

public class EmptyInboundLineException extends DomainException {
    public EmptyInboundLineException() {
        super(ErrorCode.EMPTY_INBOUND_LINE, "입고 라인이 없으면 검수를 시작할 수 없습니다.");
    }
}
