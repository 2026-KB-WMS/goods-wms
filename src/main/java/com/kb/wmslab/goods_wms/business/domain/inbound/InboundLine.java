package com.kb.wmslab.goods_wms.business.domain.inbound;

import lombok.Getter;

@Getter
public class InboundLine {

    private final Long id;
    private final Long productId;
    private final int orderedQuantity;
    private int normalQuantity;
    private int damagedQuantity;
    private int pendingInspectionQuantity;
    private boolean inspected;

    private InboundLine(Long id, Long productId, int orderedQuantity, int normalQuantity,
                        int damagedQuantity, int pendingInspectionQuantity, boolean inspected) {
        this.id = id;
        this.productId = productId;
        this.orderedQuantity = orderedQuantity;
        this.normalQuantity = normalQuantity;
        this.damagedQuantity = damagedQuantity;
        this.pendingInspectionQuantity = pendingInspectionQuantity;
        this.inspected = inspected;
    }

    public static InboundLine create(Long productId, int orderedQuantity) {
        return new InboundLine(null, productId, orderedQuantity, 0, 0, 0, false);
    }

    public static InboundLine reconstitute(Long id, Long productId, int orderedQuantity,
                                            int normalQuantity, int damagedQuantity,
                                            int pendingInspectionQuantity, boolean inspected) {
        return new InboundLine(id, productId, orderedQuantity, normalQuantity,
                damagedQuantity, pendingInspectionQuantity, inspected);
    }

    public void recordInspectionResult(int normalQuantity, int damagedQuantity, int pendingInspectionQuantity) {
        int total = normalQuantity + damagedQuantity + pendingInspectionQuantity;
        if (total != orderedQuantity) {
            throw new IllegalArgumentException(
                    "검수 수량 합계(" + total + ")가 입고 수량(" + orderedQuantity + ")과 일치하지 않습니다.");
        }
        this.normalQuantity = normalQuantity;
        this.damagedQuantity = damagedQuantity;
        this.pendingInspectionQuantity = pendingInspectionQuantity;
        this.inspected = true;
    }
}
