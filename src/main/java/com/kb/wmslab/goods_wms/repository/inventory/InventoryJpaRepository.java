package com.kb.wmslab.goods_wms.repository.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryJpaRepository extends JpaRepository<InventoryJpaEntity, Long> {
    Optional<InventoryJpaEntity> findByWarehouseIdAndZoneIdAndProductId(Long warehouseId, Long zoneId, Long productId);
    List<InventoryJpaEntity> findAllByWarehouseId(Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryJpaEntity i WHERE i.warehouseId = :warehouseId AND i.zoneId = :zoneId AND i.productId = :productId")
    Optional<InventoryJpaEntity> findByWarehouseIdAndZoneIdAndProductIdWithLock(
            @Param("warehouseId") Long warehouseId,
            @Param("zoneId") Long zoneId,
            @Param("productId") Long productId
    );
}
