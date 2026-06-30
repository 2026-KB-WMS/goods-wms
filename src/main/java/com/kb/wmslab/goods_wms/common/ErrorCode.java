package com.kb.wmslab.goods_wms.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "가용 재고가 부족합니다."),
    EXCESSIVE_RESERVATION_RELEASE(HttpStatus.BAD_REQUEST, "해제 수량이 예약 수량을 초과합니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "허용되지 않은 상태 전이입니다."),
    INSPECTION_QUANTITY_MISMATCH(HttpStatus.BAD_REQUEST, "검수 수량 합계가 입고 수량과 일치하지 않습니다."),
    EMPTY_INBOUND_LINE(HttpStatus.BAD_REQUEST, "입고 라인이 없습니다."),
    INCOMPLETE_INSPECTION(HttpStatus.BAD_REQUEST, "모든 입고 라인의 검수가 완료되어야 합니다."),
    EMPTY_OUTBOUND_LINE(HttpStatus.BAD_REQUEST, "출고 라인이 없습니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "엔티티를 찾을 수 없습니다."),
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "재고를 찾을 수 없습니다."),
    INACTIVE_ENTITY(HttpStatus.BAD_REQUEST, "비활성 상태의 엔티티입니다."),

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 유효하지 않습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT, "다른 요청이 이미 해당 데이터를 수정했습니다. 다시 시도해주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
