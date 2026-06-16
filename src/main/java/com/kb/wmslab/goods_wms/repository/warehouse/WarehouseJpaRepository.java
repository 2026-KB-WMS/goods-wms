package com.kb.wmslab.goods_wms.repository.warehouse;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseJpaRepository extends JpaRepository<WarehouseJpaEntity, Long> {

    @EntityGraph(attributePaths = {"zones"})
    @Override
    Optional<WarehouseJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"zones"})
    @Override
    List<WarehouseJpaEntity> findAll();
}
