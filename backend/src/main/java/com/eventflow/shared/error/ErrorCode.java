package com.eventflow.shared.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SUCCESS(0, "成功", HttpStatus.OK),
    INVALID_ARGUMENT(1000, "请求参数不合法", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1001, "未登录或登录已失效", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(1002, "无权执行此操作", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(1003, "请求的资源不存在", HttpStatus.NOT_FOUND),
    CONFLICT(1004, "请求与当前状态冲突", HttpStatus.CONFLICT),
    TOKEN_INVALID(1005, "访问令牌无效", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(1006, "账号或密码错误", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID(1007, "登录状态已失效，请重新登录", HttpStatus.UNAUTHORIZED),
    INTERNAL_ERROR(2000, "系统繁忙，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
