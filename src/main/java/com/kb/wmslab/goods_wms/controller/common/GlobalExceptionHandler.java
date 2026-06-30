package com.kb.wmslab.goods_wms.controller.common;

import com.kb.wmslab.goods_wms.common.DomainException;
import com.kb.wmslab.goods_wms.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException e, HttpServletRequest req) {
        ErrorCode code = e.getErrorCode();
        log4xx(code, e, req);
        return build(code, e.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log4xx(ErrorCode.VALIDATION_FAILED, e, req);
        return build(ErrorCode.VALIDATION_FAILED, message, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e, HttpServletRequest req) {
        log4xx(ErrorCode.INVALID_INPUT, e, req);
        return build(ErrorCode.INVALID_INPUT, "요청 본문을 해석할 수 없습니다.", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        String message = "파라미터 '" + e.getName() + "' 값이 올바르지 않습니다.";
        log4xx(ErrorCode.INVALID_INPUT, e, req);
        return build(ErrorCode.INVALID_INPUT, message, req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest req) {
        log4xx(ErrorCode.MISSING_PARAMETER, e, req);
        return build(ErrorCode.MISSING_PARAMETER, e.getMessage(), req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        log4xx(ErrorCode.METHOD_NOT_ALLOWED, e, req);
        return build(ErrorCode.METHOD_NOT_ALLOWED, e.getMessage(), req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e, HttpServletRequest req) {
        log4xx(ErrorCode.RESOURCE_NOT_FOUND, e, req);
        return build(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage(), req);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(ObjectOptimisticLockingFailureException e, HttpServletRequest req) {
        log4xx(ErrorCode.OPTIMISTIC_LOCK_CONFLICT, e, req);
        return build(ErrorCode.OPTIMISTIC_LOCK_CONFLICT, null, req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest req) {
        log4xx(ErrorCode.INVALID_INPUT, e, req);
        return build(ErrorCode.INVALID_INPUT, e.getMessage(), req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e, HttpServletRequest req) {
        log4xx(ErrorCode.INVALID_INPUT, e, req);
        return build(ErrorCode.INVALID_INPUT, e.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest req) {
        log.error("[5xx] {} {} - {}", req.getMethod(), req.getRequestURI(), e.getMessage(), e);
        return build(ErrorCode.INTERNAL_ERROR, null, req);
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode code, String message, HttpServletRequest req) {
        HttpStatus status = code.getStatus();
        ErrorResponse body = ErrorResponse.of(code, message, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    private void log4xx(ErrorCode code, Exception e, HttpServletRequest req) {
        log.warn("[{}] {} {} - {}", code.name(), req.getMethod(), req.getRequestURI(), e.getMessage());
    }
}
