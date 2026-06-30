package com.kb.wmslab.goods_wms.business.domain.common.exception;

import com.kb.wmslab.goods_wms.common.DomainException;
import com.kb.wmslab.goods_wms.common.ErrorCode;

public class InactiveEntityException extends DomainException {
    public InactiveEntityException(String entity, Long id) {
        super(ErrorCode.INACTIVE_ENTITY, "비활성 상태입니다: " + entity + "(id=" + id + ")");
    }
}
