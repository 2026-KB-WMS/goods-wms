package com.kb.wmslab.goods_wms.business.domain.common.exception;

public class InactiveEntityException extends DomainException {
    public InactiveEntityException(String entity, Long id) {
        super("비활성 상태입니다: " + entity + "(id=" + id + ")");
    }
}
