package com.kb.wmslab.goods_wms.repository.outbound;

import com.kb.wmslab.goods_wms.business.domain.outbound.Outbound;
import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OutboundRepositoryImpl implements OutboundRepository {

    private final OutboundJpaRepository jpaRepository;

    @Override
    public Outbound save(Outbound outbound) {
        return jpaRepository.save(OutboundJpaEntity.from(outbound)).toDomain();
    }

    @Override
    public Optional<Outbound> findById(Long id) {
        return jpaRepository.findById(id).map(OutboundJpaEntity::toDomain);
    }
}
