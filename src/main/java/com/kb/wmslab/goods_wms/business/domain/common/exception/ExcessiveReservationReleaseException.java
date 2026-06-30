package com.kb.wmslab.goods_wms.business.domain.common.exception;

import com.kb.wmslab.goods_wms.common.DomainException;
import com.kb.wmslab.goods_wms.common.ErrorCode;

public class ExcessiveReservationReleaseException extends DomainException {
    public ExcessiveReservationReleaseException(int requested, int reserved) {
        super(ErrorCode.EXCESSIVE_RESERVATION_RELEASE,
                "해제 수량(" + requested + ")이 예약 수량(" + reserved + ")을 초과합니다.");
    }
}
