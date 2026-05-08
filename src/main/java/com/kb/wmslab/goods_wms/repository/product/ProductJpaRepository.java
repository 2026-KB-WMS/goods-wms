package com.kb.wmslab.goods_wms.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {
    Optional<ProductJpaEntity> findBySku(String sku);
    boolean existsBySku(String sku);
}
