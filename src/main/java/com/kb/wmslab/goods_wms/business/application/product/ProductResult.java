package com.kb.wmslab.goods_wms.business.application.product;

import com.kb.wmslab.goods_wms.business.domain.product.Product;
import com.kb.wmslab.goods_wms.business.domain.product.ProductStatus;
import com.kb.wmslab.goods_wms.business.domain.product.ProductType;

import java.time.LocalDateTime;

public record ProductResult(
        Long id, String sku, String name, String characterName, String seriesName,
        String edition, boolean limitedEdition, ProductType productType,
        ProductStatus status, LocalDateTime createdAt
) {
    public static ProductResult from(Product product) {
        return new ProductResult(
                product.getId(), product.getSku(), product.getName(),
                product.getCharacterName(), product.getSeriesName(), product.getEdition(),
                product.isLimitedEdition(), product.getProductType(),
                product.getStatus(), product.getCreatedAt()
        );
    }
}
