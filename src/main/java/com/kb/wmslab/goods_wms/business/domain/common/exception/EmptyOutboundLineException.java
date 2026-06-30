package com.kb.wmslab.goods_wms.business.domain.common.exception;

import com.kb.wmslab.goods_wms.common.DomainException;
import com.kb.wmslab.goods_wms.common.ErrorCode;

public class EmptyOutboundLineException extends DomainException {
    public EmptyOutboundLineException() {
        super(ErrorCode.EMPTY_OUTBOUND_LINE, "출고 라인이 없으면 출고 요청을 검증할 수 없습니다.");
    }
}
