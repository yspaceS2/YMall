package com.ymall.backend.global.exception;

import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 도메인에서 명시적으로 발생시킨 비즈니스 예외를 공통 에러 응답으로 변환한다.
     * ErrorCode가 HTTP 상태와 사용자 메시지의 단일 출처가 된다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(ErrorResponse.from(errorCode));
    }

    /**
     * @Valid 검증 실패 내용을 필드 단위 메시지로 병합한다.
     * 프론트에서 어떤 입력값이 실패했는지 바로 표시할 수 있도록 field: message 형식을 사용한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        return ResponseEntity
            .status(ErrorCode.INVALID_REQUEST.getStatus())
            .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessageException() {
        return ResponseEntity
            .status(ErrorCode.INVALID_REQUEST.getStatus())
            .body(ErrorResponse.from(ErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
        ConstraintViolationException exception
    ) {
        String message = exception.getConstraintViolations()
            .stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining(", "));

        return ResponseEntity
            .status(ErrorCode.INVALID_REQUEST.getStatus())
            .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameterException(
        MissingServletRequestParameterException exception
    ) {
        String message = exception.getParameterName() + ": 필수 요청 값입니다.";

        return ResponseEntity
            .status(ErrorCode.INVALID_REQUEST.getStatus())
            .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException() {
        return ResponseEntity
            .status(ErrorCode.FILE_SIZE_EXCEEDED.getStatus())
            .body(ErrorResponse.from(ErrorCode.FILE_SIZE_EXCEEDED));
    }
}
