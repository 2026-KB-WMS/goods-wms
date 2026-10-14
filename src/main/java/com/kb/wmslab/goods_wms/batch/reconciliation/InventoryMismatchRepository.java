package com.kb.wmslab.goods_wms.batch.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMismatchRepository extends JpaRepository<InventoryMismatchJpaEntity, Long> {
}
