package com.kb.wmslab.goods_wms.business.domain.product;

import com.kb.wmslab.goods_wms.business.domain.common.exception.InactiveEntityException;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Product {

    private final Long id;
    private final String sku;
    private String name;
    private String characterName;
    private String seriesName;
    private String edition;
    private boolean limitedEdition;
    private ProductType productType;
    private ProductStatus status;
    private final LocalDateTime createdAt;
    private final Long version;

    private Product(Long id, String sku, String name, String characterName, String seriesName,
                    String edition, boolean limitedEdition, ProductType productType,
                    ProductStatus status, LocalDateTime createdAt, Long version) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.characterName = characterName;
        this.seriesName = seriesName;
        this.edition = edition;
        this.limitedEdition = limitedEdition;
        this.productType = productType;
        this.status = status;
        this.createdAt = createdAt;
        this.version = version;
    }

    public static Product create(String sku, String name, String characterName, String seriesName,
                                  String edition, boolean limitedEdition, ProductType productType) {
        return new Product(null, sku, name, characterName, seriesName, edition, limitedEdition,
                productType, ProductStatus.ACTIVE, LocalDateTime.now(), null);
    }

    public static Product reconstitute(Long id, String sku, String name, String characterName,
                                        String seriesName, String edition, boolean limitedEdition,
                                        ProductType productType, ProductStatus status,
                                        LocalDateTime createdAt, Long version) {
        return new Product(id, sku, name, characterName, seriesName, edition, limitedEdition,
                productType, status, createdAt, version);
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == ProductStatus.ACTIVE;
    }

    public void validateAvailableForTransaction() {
        if (!isActive()) throw new InactiveEntityException("Product", id);
    }
}
