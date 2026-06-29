package com.kb.wmslab.goods_wms.batch.bulkinbound;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BulkInboundProcessor implements ItemProcessor<BulkInboundLineDto, BulkInboundLineDto> {

    @Override
    public BulkInboundLineDto process(BulkInboundLineDto item) {
        if (item.getOrderedQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "orderedQuantity는 0보다 커야 합니다. productId=" + item.getProductId());
        }
        int sum = item.getNormalQuantity() + item.getDamagedQuantity() + item.getPendingInspectionQuantity();
        if (sum != item.getOrderedQuantity()) {
            throw new IllegalArgumentException(
                    "검수 수량 합계(" + sum + ")가 orderedQuantity(" + item.getOrderedQuantity()
                            + ")와 일치하지 않습니다. productId=" + item.getProductId());
        }
        return item;
    }
}
