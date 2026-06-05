package com.kb.wmslab.goods_wms.controller.product;

import com.kb.wmslab.goods_wms.business.domain.product.ProductType;

public class ProductRequest {
    public record RegisterRequest(
            String sku, String name, String characterName, String seriesName,
            String edition, boolean limitedEdition, ProductType productType
    ) {}
}
