package com.kb.wmslab.goods_wms.business.domain.common.exception;

public class ExcessiveReservationReleaseException extends DomainException {
    public ExcessiveReservationReleaseException(int requested, int reserved) {
        super("해제 수량(" + requested + ")이 예약 수량(" + reserved + ")을 초과합니다.");
    }
}
