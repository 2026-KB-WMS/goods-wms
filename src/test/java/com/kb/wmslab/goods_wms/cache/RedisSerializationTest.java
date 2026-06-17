package com.kb.wmslab.goods_wms.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.wmslab.goods_wms.business.application.inventory.InventoryResult;
import com.kb.wmslab.goods_wms.business.application.warehouse.WarehouseResult;
import com.kb.wmslab.goods_wms.business.domain.warehouse.WarehouseStatus;
import com.kb.wmslab.goods_wms.business.domain.warehouse.ZoneType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisSerializationTest {

    static class RecordSupportingTypeResolver extends ObjectMapper.DefaultTypeResolverBuilder {
        RecordSupportingTypeResolver(ObjectMapper.DefaultTyping typing, PolymorphicTypeValidator ptv) {
            super(typing, ptv);
        }

        @Override
        public boolean useForType(JavaType type) {
            if (type.getRawClass().isRecord()) return true;
            return super.useForType(type);
        }
    }

    private final InventoryResult inventory =
            new InventoryResult(1L, 10L, 100L, 5L, 50, 0, 2, 10, 40);

    private final WarehouseResult warehouse = new WarehouseResult(
            1L, "서울 창고", "서울시 강남구",
            WarehouseStatus.ACTIVE,
            new ArrayList<>(List.of(new WarehouseResult.ZoneResult(1L, "A-01", "A구역", ZoneType.NORMAL))),
            LocalDateTime.of(2026, 1, 1, 0, 0)
    );

    @Test
    void EVERYTHING_전략의_직렬화_결과를_출력한다() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfSubType(Object.class)
                                .build(),
                        ObjectMapper.DefaultTyping.EVERYTHING
                );

        System.out.println("\n========== [EVERYTHING] InventoryResult ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(inventory));

        System.out.println("\n========== [EVERYTHING] WarehouseResult ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(warehouse));

        System.out.println("\n========== [EVERYTHING] List<InventoryResult> ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(List.of(inventory)));

        System.out.println("\n========== [EVERYTHING] List<WarehouseResult> ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(List.of(warehouse)));
    }

    @Test
    void RecordSupportingTypeResolver_전략의_직렬화_결과를_출력한다() throws Exception {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.kb.wmslab.goods_wms")
                .allowIfSubType("java.util")
                .build();

        RecordSupportingTypeResolver typeResolver =
                new RecordSupportingTypeResolver(ObjectMapper.DefaultTyping.NON_FINAL, ptv);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setDefaultTyping(
                        typeResolver.init(JsonTypeInfo.Id.CLASS, null)
                                    .inclusion(JsonTypeInfo.As.WRAPPER_ARRAY)
                );

        System.out.println("\n========== [RecordSupportingTypeResolver] InventoryResult ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(inventory));

        System.out.println("\n========== [RecordSupportingTypeResolver] WarehouseResult ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(warehouse));

        System.out.println("\n========== [RecordSupportingTypeResolver] List<InventoryResult> ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new ArrayList<>(List.of(inventory))));

        System.out.println("\n========== [RecordSupportingTypeResolver] List<WarehouseResult> ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new ArrayList<>(List.of(warehouse))));
    }

    @Test
    void EVERYTHING_PROPERTY_전략의_직렬화_결과를_출력한다() throws Exception {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.kb.wmslab.goods_wms")
                .allowIfSubType("java.util")
                .build();

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);

        System.out.println("\n========== [EVERYTHING+PROPERTY] InventoryResult ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(inventory));

        System.out.println("\n========== [EVERYTHING+PROPERTY] WarehouseResult ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(warehouse));

        System.out.println("\n========== [EVERYTHING+PROPERTY] List<InventoryResult> ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(List.of(inventory)));

        System.out.println("\n========== [EVERYTHING+PROPERTY] List<WarehouseResult> ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(List.of(warehouse)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void RecordSupportingTypeResolver_직렬화_후_역직렬화_왕복_테스트() throws Exception {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.kb.wmslab.goods_wms")
                .allowIfSubType("java.util")
                .build();

        RecordSupportingTypeResolver typeResolver =
                new RecordSupportingTypeResolver(ObjectMapper.DefaultTyping.NON_FINAL, ptv);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setDefaultTyping(
                        typeResolver.init(JsonTypeInfo.Id.CLASS, null)
                                    .inclusion(JsonTypeInfo.As.WRAPPER_ARRAY)
                );

        System.out.println("\n========== [RecordSupportingTypeResolver+WRAPPER_ARRAY] InventoryResult ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(inventory));

        System.out.println("\n========== [RecordSupportingTypeResolver+WRAPPER_ARRAY] WarehouseResult ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(warehouse));

        System.out.println("\n========== [RecordSupportingTypeResolver+WRAPPER_ARRAY] List<InventoryResult> ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new ArrayList<>(List.of(inventory))));

        System.out.println("\n========== [RecordSupportingTypeResolver+WRAPPER_ARRAY] List<WarehouseResult> ==========");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new ArrayList<>(List.of(warehouse))));

        // InventoryResult
        String inventoryJson = mapper.writeValueAsString(inventory);
        InventoryResult deserializedInventory = (InventoryResult) mapper.readValue(inventoryJson, Object.class);
        assertThat(deserializedInventory).isEqualTo(inventory);

        // WarehouseResult
        String warehouseJson = mapper.writeValueAsString(warehouse);
        WarehouseResult deserializedWarehouse = (WarehouseResult) mapper.readValue(warehouseJson, Object.class);
        assertThat(deserializedWarehouse).isEqualTo(warehouse);

        // List<InventoryResult>
        List<InventoryResult> inventoryList = new ArrayList<>(List.of(inventory));
        String inventoryListJson = mapper.writeValueAsString(inventoryList);
        List<InventoryResult> deserializedInventoryList =
                (List<InventoryResult>) mapper.readValue(inventoryListJson, Object.class);
        assertThat(deserializedInventoryList).containsExactly(inventory);

        // List<WarehouseResult>
        List<WarehouseResult> warehouseList = new ArrayList<>(List.of(warehouse));
        String warehouseListJson = mapper.writeValueAsString(warehouseList);
        List<WarehouseResult> deserializedWarehouseList =
                (List<WarehouseResult>) mapper.readValue(warehouseListJson, Object.class);
        assertThat(deserializedWarehouseList).containsExactly(warehouse);
    }
}
