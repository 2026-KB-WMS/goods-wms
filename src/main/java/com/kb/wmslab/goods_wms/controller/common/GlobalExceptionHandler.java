package com.kb.wmslab.goods_wms.controller.common;

import com.kb.wmslab.goods_wms.business.domain.common.exception.EntityNotFoundException;
import com.kb.wmslab.goods_wms.business.domain.common.exception.InactiveEntityException;
import com.kb.wmslab.goods_wms.business.domain.common.exception.InsufficientStockException;
import com.kb.wmslab.goods_wms.business.domain.common.exception.InvalidStatusTransitionException;
import com.kb.wmslab.goods_wms.business.domain.common.exception.InventoryNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleInventoryNotFound(InventoryNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(InactiveEntityException.class)
    public ResponseEntity<ApiResponse<Void>> handleInactiveEntity(InactiveEntityException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStatusTransition(InvalidStatusTransitionException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStock(InsufficientStockException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
    }
}
