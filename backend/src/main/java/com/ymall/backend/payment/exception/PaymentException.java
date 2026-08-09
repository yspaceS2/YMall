package com.ymall.backend.payment.exception;

import lombok.Getter;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

@Getter
public class PaymentException extends BusinessException {

    private final String providerCode;
    private final String providerMessage;

    public PaymentException(ErrorCode errorCode, String providerCode, String providerMessage) {
        super(errorCode);
        this.providerCode = providerCode;
        this.providerMessage = providerMessage;
    }

    public PaymentException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
        this.providerCode = null;
        this.providerMessage = cause.getMessage();
    }
}
