package com.kb.wmslab.goods_wms.controller.warehouse;

import com.kb.wmslab.goods_wms.business.domain.warehouse.ZoneType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class WarehouseRequest {
    public record CreateWarehouseRequest(
            @NotBlank String name,
            @NotBlank String address
    ) {}

    public record AddZoneRequest(
            @NotBlank String zoneCode,
            @NotBlank String zoneName,
            @NotNull ZoneType zoneType
    ) {}
}
