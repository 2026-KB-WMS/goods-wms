package com.kb.wmslab.goods_wms.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryJpaRepository extends JpaRepository<InventoryJpaEntity, Long> {
    Optional<InventoryJpaEntity> findByWarehouseIdAndZoneIdAndProductId(Long warehouseId, Long zoneId, Long productId);
}
