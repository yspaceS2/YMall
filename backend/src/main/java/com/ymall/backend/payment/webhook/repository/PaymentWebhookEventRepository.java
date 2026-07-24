package com.ymall.backend.payment.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.payment.webhook.entity.PaymentWebhookEvent;

public interface PaymentWebhookEventRepository
    extends JpaRepository<PaymentWebhookEvent, Long> {

    boolean existsByTransmissionId(String transmissionId);
}
