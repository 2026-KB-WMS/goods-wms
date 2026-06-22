package com.kb.wmslab.goods_wms.business.domain.common.exception;

public class InspectionQuantityMismatchException extends DomainException {
    public InspectionQuantityMismatchException(int actualTotal, int orderedQuantity) {
        super("검수 수량 합계(" + actualTotal + ")가 입고 수량(" + orderedQuantity + ")과 일치하지 않습니다.");
    }
}
