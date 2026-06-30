package com.kb.wmslab.goods_wms.business.domain.common.exception;

import com.kb.wmslab.goods_wms.common.DomainException;
import com.kb.wmslab.goods_wms.common.ErrorCode;

public class InsufficientStockException extends DomainException {
    public InsufficientStockException(int requested, int available) {
        super(ErrorCode.INSUFFICIENT_STOCK,
                "가용 재고 부족: 요청 " + requested + ", 가용 " + available);
    }
}
