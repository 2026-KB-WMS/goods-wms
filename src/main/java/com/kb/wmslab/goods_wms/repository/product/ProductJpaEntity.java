package com.kb.wmslab.goods_wms.repository.product;

import com.kb.wmslab.goods_wms.business.domain.product.Product;
import com.kb.wmslab.goods_wms.business.domain.product.ProductStatus;
import com.kb.wmslab.goods_wms.business.domain.product.ProductType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(unique = true, nullable = false)
    private String sku;

    private String name;
    private String characterName;
    private String seriesName;
    private String edition;
    private boolean limitedEdition;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    private LocalDateTime createdAt;

    private ProductJpaEntity(String sku, String name, String characterName, String seriesName,
                              String edition, boolean limitedEdition, ProductType productType,
                              ProductStatus status, LocalDateTime createdAt) {
        this.sku = sku;
        this.name = name;
        this.characterName = characterName;
        this.seriesName = seriesName;
        this.edition = edition;
        this.limitedEdition = limitedEdition;
        this.productType = productType;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static ProductJpaEntity from(Product product) {
        ProductJpaEntity entity = new ProductJpaEntity(
                product.getSku(), product.getName(), product.getCharacterName(),
                product.getSeriesName(), product.getEdition(), product.isLimitedEdition(),
                product.getProductType(), product.getStatus(), product.getCreatedAt()
        );
        entity.id = product.getId();
        entity.version = product.getVersion();
        return entity;
    }

    public Product toDomain() {
        return Product.reconstitute(id, sku, name, characterName, seriesName,
                edition, limitedEdition, productType, status, createdAt, version);
    }
}
