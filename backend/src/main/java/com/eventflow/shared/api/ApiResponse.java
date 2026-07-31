package com.eventflow.shared.api;

import com.eventflow.shared.error.ErrorCode;
import com.eventflow.shared.request.RequestIdContext;

public record ApiResponse<T>(int code, String message, T data, String requestId) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data, RequestIdContext.current());
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), null, RequestIdContext.current());
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.code(), message, null, RequestIdContext.current());
    }
}
