package com.kb.wmslab.goods_wms.business.domain.common.exception;

public class IncompleteInspectionException extends DomainException {
    public IncompleteInspectionException() {
        super("모든 입고 라인의 검수가 완료되어야 입고를 완료할 수 있습니다.");
    }
}
