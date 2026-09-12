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
    USERNAME_EXISTS(10001, "用户名已存在", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(10002, "用户名或密码错误", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH(10006, "两次输入的密码不一致", HttpStatus.BAD_REQUEST),
    CONTENT_EMPTY(20002, "内容正文不能为空", HttpStatus.BAD_REQUEST),
    CONTENT_TOO_LONG(20003, "内容正文长度超限", HttpStatus.BAD_REQUEST),
    CATEGORY_INVALID(20004, "分类不存在", HttpStatus.BAD_REQUEST),
    TAG_LIMIT_EXCEEDED(20005, "标签数量超过限制", HttpStatus.BAD_REQUEST),
    REMARK_TOO_LONG(20006, "备注长度超限", HttpStatus.BAD_REQUEST),
    TAG_NAME_TOO_LONG(30002, "标签名称长度超限", HttpStatus.BAD_REQUEST),
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
