package com.kb.wmslab.goods_wms.controller.common;

public record ApiResponse<T>(boolean success, T data, String message) {

    private static final String DEFAULT_SUCCESS_MESSAGE = "요청이 성공적으로 처리되었습니다.";

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, DEFAULT_SUCCESS_MESSAGE);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
