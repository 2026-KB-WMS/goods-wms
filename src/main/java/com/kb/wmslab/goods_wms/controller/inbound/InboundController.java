package com.kb.wmslab.goods_wms.controller.inbound;

import com.kb.wmslab.goods_wms.business.application.inbound.InboundCommand;
import com.kb.wmslab.goods_wms.business.application.inbound.InboundResult;
import com.kb.wmslab.goods_wms.business.application.inbound.InboundUseCase;
import com.kb.wmslab.goods_wms.controller.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inbounds")
@RequiredArgsConstructor
public class InboundController {

    private final InboundUseCase inboundUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<InboundResult>> create(@RequestBody InboundRequest.CreateInboundRequest request) {
        InboundResult result = inboundUseCase.createInbound(
                new InboundCommand.CreateInbound(request.warehouseId(), request.handlerId(), request.supplierName())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(result, "입고가 생성되었습니다."));
    }

    @PostMapping("/{id}/lines")
    public ResponseEntity<ApiResponse<InboundResult>> addLine(
            @PathVariable Long id,
            @RequestBody InboundRequest.AddLineRequest request) {
        InboundResult result = inboundUseCase.addLine(id,
                new InboundCommand.AddLine(request.productId(), request.orderedQuantity())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(result, "입고 라인이 추가되었습니다."));
    }

    @PatchMapping("/{id}/inspection/start")
    public ResponseEntity<ApiResponse<InboundResult>> startInspection(@PathVariable Long id) {
        InboundResult result = inboundUseCase.startInspection(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "검수가 시작되었습니다."));
    }

    @PatchMapping("/{inboundId}/lines/{lineId}/inspection")
    public ResponseEntity<ApiResponse<InboundResult>> recordLineInspection(
            @PathVariable Long inboundId,
            @PathVariable Long lineId,
            @RequestBody InboundRequest.RecordInspectionRequest request) {
        InboundResult result = inboundUseCase.recordLineInspection(inboundId,
                new InboundCommand.RecordInspection(lineId, request.normalQuantity(), request.damagedQuantity(), request.pendingInspectionQuantity())
        );
        return ResponseEntity.ok(ApiResponse.ok(result, "검수 결과가 기록되었습니다."));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<InboundResult>> complete(@PathVariable Long id) {
        InboundResult result = inboundUseCase.completeInbound(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "입고가 완료되었습니다."));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<InboundResult>> cancel(@PathVariable Long id) {
        InboundResult result = inboundUseCase.cancelInbound(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "입고가 취소되었습니다."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InboundResult>> getInbound(@PathVariable Long id) {
        InboundResult result = inboundUseCase.getInbound(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
