package com.tradedoc.origincheck.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handle(ApiException e, HttpServletRequest req) {
        return respond(e.code(), e.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException e, HttpServletRequest req) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));
        return respond(ErrorCode.VALIDATION_ERROR, msg.isBlank() ? ErrorCode.VALIDATION_ERROR.defaultMessage() : msg, req);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> handleBadRequest(Exception e, HttpServletRequest req) {
        return respond(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(), req);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ErrorResponse> handle(MissingServletRequestPartException e, HttpServletRequest req) {
        return respond(ErrorCode.INVALID_FILE, "'" + e.getRequestPartName() + "' 파트가 필요합니다.", req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> handle(MaxUploadSizeExceededException e, HttpServletRequest req) {
        return respond(ErrorCode.FILE_TOO_LARGE, ErrorCode.FILE_TOO_LARGE.defaultMessage(), req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> handle(HttpMediaTypeNotSupportedException e, HttpServletRequest req) {
        return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type 입니다.", req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handle(NoResourceFoundException e, HttpServletRequest req) {
        return respond(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.defaultMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handle(Exception e, HttpServletRequest req) {
        log.error("처리하지 못한 오류: {} {}", req.getMethod(), req.getRequestURI(), e);
        return respond(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), req);
    }

    private static ResponseEntity<ErrorResponse> respond(ErrorCode code, String message, HttpServletRequest req) {
        return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, message, req.getRequestURI()));
    }
}
