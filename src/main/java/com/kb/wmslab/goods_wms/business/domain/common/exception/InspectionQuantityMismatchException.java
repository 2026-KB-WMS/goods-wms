package com.kb.wmslab.goods_wms.business.domain.common.exception;

import com.kb.wmslab.goods_wms.controller.common.ErrorCode;

public class InspectionQuantityMismatchException extends DomainException {
    public InspectionQuantityMismatchException(int actualTotal, int orderedQuantity) {
        super(ErrorCode.INSPECTION_QUANTITY_MISMATCH,
                "검수 수량 합계(" + actualTotal + ")가 입고 수량(" + orderedQuantity + ")과 일치하지 않습니다.");
    }
}
