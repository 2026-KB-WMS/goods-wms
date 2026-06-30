package com.kb.wmslab.goods_wms.business.domain.common.exception;

import com.kb.wmslab.goods_wms.controller.common.ErrorCode;

public class InventoryNotFoundException extends DomainException {
    public InventoryNotFoundException(Long warehouseId, Long zoneId, Long productId) {
        super(ErrorCode.INVENTORY_NOT_FOUND,
                "재고를 찾을 수 없습니다. warehouseId=" + warehouseId
                        + ", zoneId=" + zoneId + ", productId=" + productId);
    }
}
