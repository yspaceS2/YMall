package com.ymall.backend.global.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
    boolean success,
    ErrorDetail error,
    LocalDateTime timestamp
) {

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(
            false,
            new ErrorDetail(errorCode.name(), errorCode.getMessage()),
            LocalDateTime.now()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
            false,
            new ErrorDetail(errorCode.name(), message),
            LocalDateTime.now()
        );
    }

    public record ErrorDetail(
        String code,
        String message
    ) {
    }
}
