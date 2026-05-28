package com.kb.wmslab.goods_wms.business.domain.common.exception;

public class InventoryNotFoundException extends DomainException {
    public InventoryNotFoundException(Long warehouseId, Long zoneId, Long productId) {
        super("재고를 찾을 수 없습니다. warehouseId=" + warehouseId
                + ", zoneId=" + zoneId + ", productId=" + productId);
    }
}
