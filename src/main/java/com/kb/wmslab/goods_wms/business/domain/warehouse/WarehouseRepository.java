package com.kb.wmslab.goods_wms.business.domain.warehouse;

import java.util.Optional;

public interface WarehouseRepository {
    Warehouse save(Warehouse warehouse);
    Optional<Warehouse> findById(Long id);
}
