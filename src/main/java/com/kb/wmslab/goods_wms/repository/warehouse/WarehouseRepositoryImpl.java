package com.kb.wmslab.goods_wms.repository.warehouse;

import com.kb.wmslab.goods_wms.business.domain.warehouse.Warehouse;
import com.kb.wmslab.goods_wms.business.domain.warehouse.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WarehouseRepositoryImpl implements WarehouseRepository {

    private final WarehouseJpaRepository jpaRepository;

    @Override
    public Warehouse save(Warehouse warehouse) {
        return jpaRepository.save(WarehouseJpaEntity.from(warehouse)).toDomain();
    }

    @Override
    public Optional<Warehouse> findById(Long id) {
        return jpaRepository.findById(id).map(WarehouseJpaEntity::toDomain);
    }
}
