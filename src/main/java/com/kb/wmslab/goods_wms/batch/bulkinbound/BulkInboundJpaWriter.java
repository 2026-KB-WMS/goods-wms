package com.kb.wmslab.goods_wms.batch.bulkinbound;

import com.kb.wmslab.goods_wms.business.domain.inbound.Inbound;
import com.kb.wmslab.goods_wms.business.domain.inbound.InboundLine;
import com.kb.wmslab.goods_wms.repository.inbound.InboundJpaEntity;
import com.kb.wmslab.goods_wms.repository.inbound.InboundJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BulkInboundJpaWriter implements ItemWriter<BulkInboundLineDto> {

    private final InboundJpaRepository inboundJpaRepository;

    @Override
    public void write(Chunk<? extends BulkInboundLineDto> chunk) {
        Map<MasterKey, List<BulkInboundLineDto>> grouped = new LinkedHashMap<>();
        for (BulkInboundLineDto line : chunk) {
            MasterKey key = new MasterKey(line.getWarehouseId(), line.getHandlerId(), line.getSupplierName());
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(line);
        }

        List<InboundJpaEntity> entities = new ArrayList<>(grouped.size());
        for (Map.Entry<MasterKey, List<BulkInboundLineDto>> e : grouped.entrySet()) {
            entities.add(toEntity(e.getKey(), e.getValue()));
        }
        inboundJpaRepository.saveAll(entities);
    }

    private InboundJpaEntity toEntity(MasterKey key, List<BulkInboundLineDto> lines) {
        Inbound inbound = Inbound.create(key.warehouseId(), key.handlerId(), key.supplierName());
        lines.forEach(line -> {
            InboundLine inboundLine = InboundLine.create(line.getProductId(), line.getOrderedQuantity());
            inboundLine.recordInspectionResult(
                    line.getNormalQuantity(), line.getDamagedQuantity(), line.getPendingInspectionQuantity());
            inbound.addLine(inboundLine);
        });
        inbound.startInspection();
        inbound.complete();
        return InboundJpaEntity.from(inbound);
    }

    private record MasterKey(Long warehouseId, Long handlerId, String supplierName) {}
}
