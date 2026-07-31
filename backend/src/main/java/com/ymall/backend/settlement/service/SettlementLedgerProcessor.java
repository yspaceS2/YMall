package com.ymall.backend.settlement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.messaging.OrderEventEnvelope;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.refund.entity.PaymentRefund;
import com.ymall.backend.payment.refund.entity.PaymentRefundItem;
import com.ymall.backend.payment.refund.repository.PaymentRefundRepository;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.settlement.config.SettlementProperties;
import com.ymall.backend.settlement.entity.SettlementEntryType;
import com.ymall.backend.settlement.entity.SettlementLedgerEntry;
import com.ymall.backend.settlement.entity.SettlementStatus;
import com.ymall.backend.settlement.repository.SettlementLedgerRepository;

@Service
@RequiredArgsConstructor
public class SettlementLedgerProcessor {

    private static final int MONEY_SCALE = 2;

    private final SettlementLedgerRepository ledgerRepository;
    private final OrderRepository orderRepository;
    private final PaymentRefundRepository refundRepository;
    private final SettlementProperties properties;

    @Transactional
    public void process(OrderEventEnvelope event) {
        switch (event.eventType()) {
            case PAYMENT_COMPLETED -> recordSales(event);
            case REFUND_COMPLETED -> recordRefund(event);
            case ORDER_DELIVERED -> makeEntriesAvailable(event.orderId());
            default -> {
                // This consumer shares the order event topic and intentionally ignores other events.
            }
        }
    }

    private void recordSales(OrderEventEnvelope event) {
        Order order = orderRepository.findByIdForSettlement(event.orderId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.getItems().forEach(item -> sellerProfile(item)
            .ifPresent(sellerProfile -> saveSale(event, sellerProfile, item)));
    }

    private void saveSale(
        OrderEventEnvelope event,
        SellerProfile sellerProfile,
        OrderItem item
    ) {
        String sourceKey = "SALE:" + item.getId();
        if (ledgerRepository.existsBySourceKey(sourceKey)) {
            return;
        }
        BigDecimal grossAmount = item.getLineTotal().add(item.getShippingFee());
        BigDecimal feeAmount = calculateFee(item.getUnitPrice())
            .multiply(BigDecimal.valueOf(item.getQuantity()));
        ledgerRepository.save(SettlementLedgerEntry.sale(
            sellerProfile,
            item,
            grossAmount,
            feeAmount,
            event.eventId(),
            event.occurredAt()
        ));
    }

    private void recordRefund(OrderEventEnvelope event) {
        Long refundId = payloadLong(event, "refundId");
        PaymentRefund refund = refundRepository.findByIdForSettlement(refundId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_FOUND));
        refund.getItems().forEach(refundItem -> sellerProfile(refundItem.getOrderItem())
            .ifPresent(sellerProfile ->
                saveRefund(event, sellerProfile, refund, refundItem)
            ));
    }

    private void saveRefund(
        OrderEventEnvelope event,
        SellerProfile sellerProfile,
        PaymentRefund refund,
        PaymentRefundItem refundItem
    ) {
        OrderItem orderItem = refundItem.getOrderItem();
        String sourceKey = "REFUND:" + refund.getId() + ":" + orderItem.getId();
        if (ledgerRepository.existsBySourceKey(sourceKey)) {
            return;
        }
        BigDecimal feeAmount = calculateFee(orderItem.getUnitPrice())
            .multiply(BigDecimal.valueOf(refundItem.getQuantity()));
        SettlementLedgerEntry entry = SettlementLedgerEntry.refund(
            sellerProfile,
            refund,
            orderItem,
            refundItem.getAmount(),
            feeAmount,
            event.eventId(),
            event.occurredAt()
        );
        if (ledgerRepository.existsByOrderItemIdAndEntryTypeAndStatusIn(
            orderItem.getId(),
            SettlementEntryType.SALE,
            java.util.List.of(
                SettlementStatus.AVAILABLE,
                SettlementStatus.REQUESTED,
                SettlementStatus.PAID
            )
        )) {
            entry.makeAvailable();
        }
        ledgerRepository.save(entry);
    }

    private void makeEntriesAvailable(Long orderId) {
        ledgerRepository.findAllByOrderIdAndStatus(
            orderId,
            SettlementStatus.PENDING
        ).forEach(SettlementLedgerEntry::makeAvailable);
    }

    private BigDecimal calculateFee(BigDecimal unitPrice) {
        return unitPrice.multiply(properties.feeRate())
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private Optional<SellerProfile> sellerProfile(OrderItem item) {
        return Optional.ofNullable(item.getProduct().getSellerProfile());
    }

    private Long payloadLong(OrderEventEnvelope event, String key) {
        Object value = event.payload().get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                // Handled as an invalid event below.
            }
        }
        throw new IllegalArgumentException("Order event payload is missing " + key + ".");
    }
}
