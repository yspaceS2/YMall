package com.ymall.backend.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
    PASSWORD_RESET_REQUEST_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 요청해 주세요."),
    PASSWORD_RESET_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않거나 만료되었습니다."),
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "비밀번호 재설정 요청이 올바르지 않거나 만료되었습니다."),
    MEMBER_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "배송지를 찾을 수 없습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    REVIEW_NOT_ALLOWED(HttpStatus.CONFLICT, "배송이 완료된 구매 상품만 리뷰를 작성할 수 있습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 리뷰를 작성한 주문 상품입니다."),
    MEMBER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    OAUTH_EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "일반 로그인으로 가입된 이메일입니다."),
    OAUTH_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 다른 회원 또는 현재 회원에 연결된 소셜 계정입니다."),
    OAUTH_REQUIRED_INFORMATION_MISSING(HttpStatus.BAD_REQUEST, "소셜 계정의 필수 정보 제공에 동의해 주세요."),
    OAUTH_EMAIL_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "이메일 인증을 완료해 주세요."),
    OAUTH_EMAIL_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "이메일 인증번호가 올바르지 않거나 만료되었습니다."),
    OAUTH_EMAIL_DELIVERY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "인증 이메일을 발송하지 못했습니다."),
    GOOGLE_ONE_TAP_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Google 로그인 정보가 올바르지 않거나 만료되었습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 인증 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 로그인 세션입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_REVIEW_NOT_ALLOWED(HttpStatus.CONFLICT, "이미 심사가 완료된 상품입니다."),
    PRODUCT_NOT_AVAILABLE(HttpStatus.CONFLICT, "현재 장바구니에 담을 수 없는 상품입니다."),
    PRODUCT_NOT_ORDERABLE(HttpStatus.CONFLICT, "현재 주문할 수 없는 상품입니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "상품 재고가 부족합니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니 상품을 찾을 수 없습니다."),
    CART_EMPTY(HttpStatus.CONFLICT, "장바구니가 비어 있습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    PAYMENT_NOT_ALLOWED(HttpStatus.CONFLICT, "현재 상태에서는 결제를 처리할 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 요청 금액이 주문 금액과 일치하지 않습니다."),
    PAYMENT_ORDER_MISMATCH(HttpStatus.BAD_REQUEST, "결제 주문번호가 일치하지 않습니다."),
    PAYMENT_KEY_CONFLICT(HttpStatus.CONFLICT, "이미 다른 주문에 사용된 결제 키입니다."),
    PAYMENT_GATEWAY_CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "결제사 연동 설정이 올바르지 않습니다."),
    PAYMENT_GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "결제사 응답 시간이 초과되었습니다."),
    PAYMENT_GATEWAY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "결제사에 연결할 수 없습니다."),
    PAYMENT_GATEWAY_ERROR(HttpStatus.BAD_GATEWAY, "결제사 요청 처리에 실패했습니다."),
    PAYMENT_WEBHOOK_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 결제 웹훅입니다."),
    PAYMENT_WEBHOOK_UNSUPPORTED_EVENT(HttpStatus.BAD_REQUEST, "지원하지 않는 결제 웹훅 이벤트입니다."),
    PAYMENT_REFUND_NOT_ALLOWED(HttpStatus.CONFLICT, "현재 상태에서는 환불할 수 없습니다."),
    PAYMENT_REFUND_AMOUNT_EXCEEDED(HttpStatus.CONFLICT, "환불 가능 금액 또는 수량을 초과했습니다."),
    PAYMENT_REFUND_NOT_FOUND(HttpStatus.NOT_FOUND, "환불 이력을 찾을 수 없습니다."),
    PAYMENT_REFUND_RECONCILIATION_REQUIRED(
        HttpStatus.CONFLICT,
        "처리 중이거나 결과를 확인 중인 환불이 있어 새 환불을 요청할 수 없습니다."
    ),
    PAYMENT_REFUND_PROVIDER_MISMATCH(
        HttpStatus.BAD_GATEWAY,
        "결제사의 환불 결과가 요청 내용과 일치하지 않습니다."
    ),
    ORDER_CANCELLATION_NOT_ALLOWED(HttpStatus.CONFLICT, "현재 상태에서는 주문을 취소할 수 없습니다."),
    SELLER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "판매자 프로필을 찾을 수 없습니다."),
    SELLER_PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 판매자 프로필이 존재합니다."),
    SELLER_BUSINESS_NUMBER_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 사업자 번호입니다."),
    SELLER_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "판매자의 상품을 찾을 수 없습니다."),
    SELLER_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "판매자의 주문을 찾을 수 없습니다."),
    ORDER_FULFILLMENT_NOT_ALLOWED(HttpStatus.CONFLICT, "현재 상태에서는 배송 상태를 변경할 수 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다."),
    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
