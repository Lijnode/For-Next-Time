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
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(Result.failure(errorCode.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse(ErrorCode.PARAM_ERROR.getMessage());
        return ResponseEntity.badRequest().body(Result.failure(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Result<Void>> handleRequestException(Exception exception) {
        ErrorCode errorCode = exception instanceof HttpMessageNotReadableException
                ? ErrorCode.REQUEST_FORMAT_ERROR
                : ErrorCode.PARAM_ERROR;
        return ResponseEntity.status(errorCode.getStatus()).body(Result.failure(errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled system exception", exception);
        return ResponseEntity.status(ErrorCode.SYSTEM_ERROR.getStatus()).body(Result.failure(ErrorCode.SYSTEM_ERROR));
    }
}
