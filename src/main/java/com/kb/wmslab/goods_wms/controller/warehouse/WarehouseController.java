package com.kb.wmslab.goods_wms.controller.warehouse;

import com.kb.wmslab.goods_wms.business.application.warehouse.WarehouseCommand;
import com.kb.wmslab.goods_wms.business.application.warehouse.WarehouseResult;
import com.kb.wmslab.goods_wms.business.application.warehouse.WarehouseUseCase;
import com.kb.wmslab.goods_wms.controller.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseUseCase warehouseUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseResult>> create(@RequestBody WarehouseRequest.CreateWarehouseRequest request) {
        WarehouseResult result = warehouseUseCase.createWarehouse(
                new WarehouseCommand.CreateWarehouse(request.name(), request.address())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(result, "창고가 생성되었습니다."));
    }

    @PostMapping("/{id}/zones")
    public ResponseEntity<ApiResponse<WarehouseResult>> addZone(
            @PathVariable Long id,
            @RequestBody WarehouseRequest.AddZoneRequest request) {
        WarehouseResult result = warehouseUseCase.addZone(id,
                new WarehouseCommand.AddZone(request.zoneCode(), request.zoneName(), request.zoneType())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(result, "구역이 추가되었습니다."));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<WarehouseResult>> activate(@PathVariable Long id) {
        WarehouseResult result = warehouseUseCase.activateWarehouse(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "창고가 활성화되었습니다."));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<WarehouseResult>> deactivate(@PathVariable Long id) {
        WarehouseResult result = warehouseUseCase.deactivateWarehouse(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "창고가 비활성화되었습니다."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseResult>> getWarehouse(@PathVariable Long id) {
        WarehouseResult result = warehouseUseCase.getWarehouse(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
