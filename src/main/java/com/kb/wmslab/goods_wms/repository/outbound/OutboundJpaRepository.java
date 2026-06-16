package com.kb.wmslab.goods_wms.repository.outbound;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OutboundJpaRepository extends JpaRepository<OutboundJpaEntity, Long> {

    @EntityGraph(attributePaths = {"lines"})
    @Override
    Optional<OutboundJpaEntity> findById(Long id);
}
