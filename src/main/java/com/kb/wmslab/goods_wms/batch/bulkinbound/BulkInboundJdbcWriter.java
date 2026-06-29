package com.kb.wmslab.goods_wms.batch.bulkinbound;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BulkInboundJdbcWriter implements ItemWriter<BulkInboundLineDto> {

    private static final String LINE_INSERT_SQL =
            "INSERT INTO inbound_lines " +
            "(version, inbound_id, product_id, ordered_quantity, " +
            " normal_quantity, damaged_quantity, pending_inspection_quantity, inspected) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert inboundInsert;

    public BulkInboundJdbcWriter(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.inboundInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("inbounds")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public void write(Chunk<? extends BulkInboundLineDto> chunk) {
        Map<MasterKey, List<BulkInboundLineDto>> grouped = new LinkedHashMap<>();
        for (BulkInboundLineDto line : chunk) {
            MasterKey key = new MasterKey(line.getWarehouseId(), line.getHandlerId(), line.getSupplierName());
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(line);
        }

        Map<MasterKey, Long> masterIds = insertMasters(grouped.keySet());
        insertLinesInBatch(grouped, masterIds);
    }

    private Map<MasterKey, Long> insertMasters(java.util.Set<MasterKey> keys) {
        Map<MasterKey, Long> ids = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (MasterKey key : keys) {
            Map<String, Object> params = new HashMap<>();
            params.put("version", 0L);
            params.put("warehouse_id", key.warehouseId());
            params.put("handler_id", key.handlerId());
            params.put("supplier_name", key.supplierName());
            params.put("status", "COMPLETED");
            params.put("created_at", now);
            Number id = inboundInsert.executeAndReturnKey(params);
            ids.put(key, id.longValue());
        }
        return ids;
    }

    private void insertLinesInBatch(Map<MasterKey, List<BulkInboundLineDto>> grouped, Map<MasterKey, Long> masterIds) {
        List<Object[]> batchArgs = new ArrayList<>();
        for (Map.Entry<MasterKey, List<BulkInboundLineDto>> e : grouped.entrySet()) {
            Long inboundId = masterIds.get(e.getKey());
            for (BulkInboundLineDto line : e.getValue()) {
                batchArgs.add(new Object[]{
                        0L,
                        inboundId,
                        line.getProductId(),
                        line.getOrderedQuantity(),
                        line.getNormalQuantity(),
                        line.getDamagedQuantity(),
                        line.getPendingInspectionQuantity(),
                        true
                });
            }
        }
        jdbcTemplate.batchUpdate(LINE_INSERT_SQL, batchArgs);
    }

    private record MasterKey(Long warehouseId, Long handlerId, String supplierName) {}
}
