package com.kb.wmslab.goods_wms.business.domain.common.exception;

public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(String entity, Long id) {
        super("해당 엔티티가 존재하지 않습니다. Entity: " + entity + "(id=" + id + ")");
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}
