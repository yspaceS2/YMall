package com.ymall.backend.payment.webhook.entity;

public enum PaymentWebhookProcessingResult {
    APPLIED,
    NO_CHANGE,
    STALE_EVENT
}
