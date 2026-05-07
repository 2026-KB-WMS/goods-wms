package com.kb.wmslab.goods_wms.business.domain.inbound;

import java.util.Optional;

public interface InboundRepository {
    Inbound save(Inbound inbound);
    Optional<Inbound> findById(Long id);
}
