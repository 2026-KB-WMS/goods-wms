package com.kb.wmslab.goods_wms.business.domain.outbound;

import lombok.Getter;

@Getter
public class OutboundLine {

    private final Long id;
    private final Long productId;
    private final int quantity;

    private OutboundLine(Long id, Long productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("출고 수량은 0보다 커야 합니다.");
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
    }

    public static OutboundLine create(Long productId, int quantity) {
        return new OutboundLine(null, productId, quantity);
    }

    public static OutboundLine reconstitute(Long id, Long productId, int quantity) {
        return new OutboundLine(id, productId, quantity);
    }
}
