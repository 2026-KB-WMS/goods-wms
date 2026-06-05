package com.kb.wmslab.goods_wms.controller.product;

import com.kb.wmslab.goods_wms.business.application.product.ProductCommand;
import com.kb.wmslab.goods_wms.business.application.product.ProductResult;
import com.kb.wmslab.goods_wms.business.application.product.ProductUseCase;
import com.kb.wmslab.goods_wms.controller.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductUseCase productUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResult>> register(@Valid @RequestBody ProductRequest.RegisterRequest request) {
        ProductResult result = productUseCase.registerProduct(
                new ProductCommand.Register(
                        request.sku(), request.name(), request.characterName(), request.seriesName(),
                        request.edition(), request.limitedEdition(), request.productType()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(result, "상품이 등록되었습니다."));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ProductResult>> activate(@PathVariable Long id) {
        ProductResult result = productUseCase.activateProduct(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "상품이 활성화되었습니다."));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<ProductResult>> deactivate(@PathVariable Long id) {
        ProductResult result = productUseCase.deactivateProduct(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "상품이 비활성화되었습니다."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResult>> getProduct(@PathVariable Long id) {
        ProductResult result = productUseCase.getProduct(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
