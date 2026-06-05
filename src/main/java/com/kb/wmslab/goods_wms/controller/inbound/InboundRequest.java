package com.kb.wmslab.goods_wms.controller.inbound;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class InboundRequest {
    public record CreateInboundRequest(
            @NotNull Long warehouseId,
            @NotNull Long handlerId,
            @NotBlank String supplierName
    ) {}

    public record AddLineRequest(
            @NotNull Long productId,
            @Positive int orderedQuantity
    ) {}

    public record RecordInspectionRequest(
            @PositiveOrZero int normalQuantity,
            @PositiveOrZero int damagedQuantity,
            @PositiveOrZero int pendingInspectionQuantity
    ) {}
}
