package com.kb.wmslab.goods_wms.business.application.product;

import com.kb.wmslab.goods_wms.business.domain.product.ProductType;

public class ProductCommand {
    public record Register(
            String sku, String name, String characterName, String seriesName,
            String edition, boolean limitedEdition, ProductType productType
    ) {}
}
