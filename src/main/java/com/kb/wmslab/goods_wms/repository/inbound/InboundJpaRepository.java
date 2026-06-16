package com.kb.wmslab.goods_wms.repository.inbound;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InboundJpaRepository extends JpaRepository<InboundJpaEntity, Long> {

    @EntityGraph(attributePaths = {"lines"})
    @Override
    Optional<InboundJpaEntity> findById(Long id);
}
