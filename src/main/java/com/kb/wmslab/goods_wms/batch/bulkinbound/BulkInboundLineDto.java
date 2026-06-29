package com.kb.wmslab.goods_wms.batch.bulkinbound;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BulkInboundLineDto {
    private Long warehouseId;
    private Long handlerId;
    private String supplierName;
    private Long productId;
    private int orderedQuantity;
    private int normalQuantity;
    private int damagedQuantity;
    private int pendingInspectionQuantity;
}
