package com.ymall.backend.payment.refund.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import com.ymall.backend.payment.refund.dto.PaymentRefundItemResponse;
import com.ymall.backend.payment.refund.dto.PaymentRefundResponse;
import com.ymall.backend.payment.refund.entity.PaymentRefund;
import com.ymall.backend.payment.refund.entity.PaymentRefundItem;

@Component
class PaymentRefundResponseMapper {

    PaymentRefundResponse toResponse(PaymentRefund refund) {
        return toResponse(refund, item -> true);
    }

    PaymentRefundResponse toResponse(
        PaymentRefund refund,
        Predicate<PaymentRefundItem> itemAccess
    ) {
        List<PaymentRefundItemResponse> items = refund.getItems().stream()
            .filter(itemAccess)
            .map(item -> new PaymentRefundItemResponse(
                item.getOrderItem().getId(),
                item.getOrderItem().getProductName(),
                item.getQuantity(),
                item.getAmount()
            ))
            .toList();
        return new PaymentRefundResponse(
            refund.getId(),
            refund.getOrder().getId(),
            refund.getType(),
            refund.getStatus(),
            items.stream()
                .map(PaymentRefundItemResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add),
            refund.getReason(),
            refund.getFailureMessage(),
            items,
            refund.getCreatedAt(),
            refund.getProcessedAt()
        );
    }
}
