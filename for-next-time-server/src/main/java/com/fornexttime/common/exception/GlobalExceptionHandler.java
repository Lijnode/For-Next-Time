package com.fornexttime.common.exception;

import com.fornexttime.common.model.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    /**
     * 将可预期的业务异常转换为统一响应。
     */
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        // 1. 读取业务异常定义的状态码与业务码。
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(Result.failure(errorCode.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    /**
     * 将 DTO 参数校验失败转换为统一的参数错误响应。
     */
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        // 1. 取第一个可读的校验失败信息。
        String message = exception.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse(ErrorCode.PARAM_ERROR.getMessage());
        return ResponseEntity.badRequest().body(Result.failure(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    /**
     * 处理约束校验和 JSON 格式错误。
     */
    public ResponseEntity<Result<Void>> handleRequestException(Exception exception) {
        // 1. 根据异常类型选择参数或请求格式错误码。
        ErrorCode errorCode = exception instanceof HttpMessageNotReadableException
                ? ErrorCode.REQUEST_FORMAT_ERROR
                : ErrorCode.PARAM_ERROR;
        return ResponseEntity.status(errorCode.getStatus()).body(Result.failure(errorCode));
    }

    @ExceptionHandler(Exception.class)
    /**
     * 记录未预期异常并避免向客户端泄露内部细节。
     */
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception exception) {
        // 1. 仅在服务端记录完整异常堆栈。
        log.error("Unhandled system exception", exception);
        // 2. 向客户端返回通用系统错误。
        return ResponseEntity.status(ErrorCode.SYSTEM_ERROR.getStatus()).body(Result.failure(ErrorCode.SYSTEM_ERROR));
    }
}
