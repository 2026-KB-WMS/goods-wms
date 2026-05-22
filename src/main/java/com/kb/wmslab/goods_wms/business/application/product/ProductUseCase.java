package com.kb.wmslab.goods_wms.business.application.product;

public interface ProductUseCase {
    ProductResult registerProduct(ProductCommand.Register command);
    ProductResult activateProduct(Long id);
    ProductResult deactivateProduct(Long id);
    ProductResult getProduct(Long id);
}
