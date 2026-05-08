package com.kb.wmslab.goods_wms.repository.inbound;

import com.kb.wmslab.goods_wms.business.domain.inbound.Inbound;
import com.kb.wmslab.goods_wms.business.domain.inbound.InboundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InboundRepositoryImpl implements InboundRepository {

    private final InboundJpaRepository jpaRepository;

    @Override
    public Inbound save(Inbound inbound) {
        return jpaRepository.save(InboundJpaEntity.from(inbound)).toDomain();
    }

    @Override
    public Optional<Inbound> findById(Long id) {
        return jpaRepository.findById(id).map(InboundJpaEntity::toDomain);
    }
}
