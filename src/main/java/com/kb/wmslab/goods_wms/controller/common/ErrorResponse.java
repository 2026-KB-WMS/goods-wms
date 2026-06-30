package com.kb.wmslab.goods_wms.controller.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ErrorResponse(
        boolean success,
        String errorCode,
        String message,
        String path,
        String timestamp
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static ErrorResponse of(ErrorCode code, String message, String path) {
        return new ErrorResponse(
                false,
                code.name(),
                message != null ? message : code.getDefaultMessage(),
                path,
                LocalDateTime.now().format(FORMATTER)
        );
    }
}
