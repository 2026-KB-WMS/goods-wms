package com.kb.wmslab.goods_wms.business.domain.outbound;

import java.util.Optional;

public interface OutboundRepository {
    Outbound save(Outbound outbound);
    Optional<Outbound> findById(Long id);
}
