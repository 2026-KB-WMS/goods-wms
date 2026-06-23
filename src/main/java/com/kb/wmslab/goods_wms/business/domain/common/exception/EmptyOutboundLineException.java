package com.kb.wmslab.goods_wms.business.domain.common.exception;

public class EmptyOutboundLineException extends DomainException {
    public EmptyOutboundLineException() {
        super("출고 라인이 없으면 출고 요청을 검증할 수 없습니다.");
    }
}
