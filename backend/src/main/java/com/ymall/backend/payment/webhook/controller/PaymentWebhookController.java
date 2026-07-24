package com.ymall.backend.payment.webhook.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.payment.webhook.dto.TossPaymentWebhookRequest;
import com.ymall.backend.payment.webhook.service.PaymentWebhookService;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/webhooks/toss")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping
    public ResponseEntity<Void> receive(
        @RequestHeader("tosspayments-webhook-transmission-id")
        @NotBlank
        @Size(max = 200)
        String transmissionId,
        @Valid @RequestBody TossPaymentWebhookRequest request
    ) {
        paymentWebhookService.handle(transmissionId, request);
        return ResponseEntity.ok().build();
    }
}
