package com.kb.wmslab.goods_wms.controller.product;

import com.kb.wmslab.goods_wms.business.domain.product.ProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProductRequest {
    public record RegisterRequest(
            @NotBlank String sku,
            @NotBlank String name,
            @NotBlank String characterName,
            @NotBlank String seriesName,
            @NotBlank String edition,
            boolean limitedEdition,
            @NotNull ProductType productType
    ) {}
}
