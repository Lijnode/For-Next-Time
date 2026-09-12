package com.fornexttime.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    PARAM_ERROR(40000, "请求参数错误", HttpStatus.BAD_REQUEST),
    REQUEST_FORMAT_ERROR(40001, "请求数据格式错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "未登录", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(40101, "Token 无效", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(40102, "Token 已过期", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "无权操作", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(40400, "资源不存在", HttpStatus.NOT_FOUND),
    SYSTEM_ERROR(50000, "系统异常", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
