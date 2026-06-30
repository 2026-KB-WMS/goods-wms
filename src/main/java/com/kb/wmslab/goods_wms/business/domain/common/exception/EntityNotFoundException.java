package com.kb.wmslab.goods_wms.business.domain.common.exception;

import com.kb.wmslab.goods_wms.common.DomainException;
import com.kb.wmslab.goods_wms.common.ErrorCode;

public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(String entity, Long id) {
        super(ErrorCode.ENTITY_NOT_FOUND,
                "해당 엔티티가 존재하지 않습니다. Entity: " + entity + "(id=" + id + ")");
    }

    public EntityNotFoundException(String message) {
        super(ErrorCode.ENTITY_NOT_FOUND, message);
    }
}
