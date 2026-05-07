package com.kb.wmslab.goods_wms.business.domain.common.exception;

public class InvalidStatusTransitionException extends DomainException {
    public InvalidStatusTransitionException(String from, String to) {
        super("상태 전이 불가: " + from + " → " + to);
    }
}
