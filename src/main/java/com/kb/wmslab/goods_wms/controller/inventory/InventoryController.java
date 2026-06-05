package com.kb.wmslab.goods_wms.controller.inventory;

import com.kb.wmslab.goods_wms.business.application.inventory.InventoryResult;
import com.kb.wmslab.goods_wms.business.application.inventory.InventoryUseCase;
import com.kb.wmslab.goods_wms.controller.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryUseCase inventoryUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<InventoryResult>> getInventory(
            @RequestParam Long warehouseId,
            @RequestParam Long zoneId,
            @RequestParam Long productId) {
        InventoryResult result = inventoryUseCase.getInventory(warehouseId, zoneId, productId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/warehouses/{warehouseId}")
    public ResponseEntity<ApiResponse<List<InventoryResult>>> getInventoriesByWarehouse(
            @PathVariable Long warehouseId) {
        List<InventoryResult> result = inventoryUseCase.getInventoriesByWarehouse(warehouseId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
