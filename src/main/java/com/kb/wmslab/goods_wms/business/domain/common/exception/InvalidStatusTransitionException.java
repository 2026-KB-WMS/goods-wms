package com.kb.wmslab.goods_wms.business.domain.common.exception;

import com.kb.wmslab.goods_wms.common.DomainException;
import com.kb.wmslab.goods_wms.common.ErrorCode;

public class InvalidStatusTransitionException extends DomainException {
    public InvalidStatusTransitionException(String from, String to) {
        super(ErrorCode.INVALID_STATUS_TRANSITION, "상태 전이 불가: " + from + " → " + to);
    }
}
