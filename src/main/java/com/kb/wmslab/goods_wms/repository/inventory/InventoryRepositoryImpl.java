package com.kb.wmslab.goods_wms.repository.inventory;

import com.kb.wmslab.goods_wms.business.domain.inventory.Inventory;
import com.kb.wmslab.goods_wms.business.domain.inventory.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepository {

    private final InventoryJpaRepository jpaRepository;

    @Override
    public Inventory save(Inventory inventory) {
        return jpaRepository.save(InventoryJpaEntity.from(inventory)).toDomain();
    }

    @Override
    public Optional<Inventory> findByWarehouseIdAndZoneIdAndProductId(Long warehouseId, Long zoneId, Long productId) {
        return jpaRepository.findByWarehouseIdAndZoneIdAndProductId(warehouseId, zoneId, productId)
                .map(InventoryJpaEntity::toDomain);
    }
}
