package com.kb.wmslab.goods_wms.business.domain.common.exception;

public class InsufficientStockException extends DomainException {
    public InsufficientStockException(int requested, int available) {
        super("가용 재고 부족: 요청 " + requested + ", 가용 " + available);
    }
}
