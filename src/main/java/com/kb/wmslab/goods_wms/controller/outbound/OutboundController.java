package com.kb.wmslab.goods_wms.controller.outbound;

import com.kb.wmslab.goods_wms.business.application.outbound.OutboundCommand;
import com.kb.wmslab.goods_wms.business.application.outbound.OutboundResult;
import com.kb.wmslab.goods_wms.business.application.outbound.OutboundUseCase;
import com.kb.wmslab.goods_wms.controller.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/outbounds")
@RequiredArgsConstructor
public class OutboundController {

    private final OutboundUseCase outboundUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<OutboundResult>> create(@RequestBody OutboundRequest.CreateOutboundRequest request) {
        OutboundResult result = outboundUseCase.createOutbound(
                new OutboundCommand.CreateOutbound(request.warehouseId(), request.handlerId(), request.destination(), request.purpose())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(result, "출고가 생성되었습니다."));
    }

    @PostMapping("/{id}/lines")
    public ResponseEntity<ApiResponse<OutboundResult>> addLine(
            @PathVariable Long id,
            @RequestBody OutboundRequest.AddLineRequest request) {
        OutboundResult result = outboundUseCase.addLine(id,
                new OutboundCommand.AddLine(request.productId(), request.quantity())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(result, "출고 라인이 추가되었습니다."));
    }

    @PatchMapping("/{id}/validate")
    public ResponseEntity<ApiResponse<OutboundResult>> validate(@PathVariable Long id) {
        OutboundResult result = outboundUseCase.validateOutbound(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "출고가 검증되었습니다."));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<OutboundResult>> complete(@PathVariable Long id) {
        OutboundResult result = outboundUseCase.completeOutbound(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "출고가 완료되었습니다."));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OutboundResult>> cancel(@PathVariable Long id) {
        OutboundResult result = outboundUseCase.cancelOutbound(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "출고가 취소되었습니다."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OutboundResult>> getOutbound(@PathVariable Long id) {
        OutboundResult result = outboundUseCase.getOutbound(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
