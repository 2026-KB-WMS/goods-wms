package com.kb.wmslab.goods_wms.controller.outbound;

import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class OutboundRequest {
    public record CreateOutboundRequest(
            @NotNull Long warehouseId,
            @NotNull Long handlerId,
            @NotBlank String destination,
            @NotNull OutboundPurpose purpose
    ) {}

    public record AddLineRequest(
            @NotNull Long productId,
            @Positive int quantity
    ) {}
}
