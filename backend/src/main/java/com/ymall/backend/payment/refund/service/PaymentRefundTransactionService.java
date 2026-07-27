package com.ymall.backend.payment.refund.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.messaging.OrderEventType;
import com.ymall.backend.global.messaging.outbox.OrderOutboxService;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.entity.Payment;
import com.ymall.backend.payment.entity.PaymentResult;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;
import com.ymall.backend.payment.refund.dto.PaymentRefundItemRequest;
import com.ymall.backend.payment.refund.dto.PaymentRefundItemResponse;
import com.ymall.backend.payment.refund.dto.PaymentRefundRequest;
import com.ymall.backend.payment.refund.dto.PaymentRefundResponse;
import com.ymall.backend.payment.refund.entity.PaymentRefund;
import com.ymall.backend.payment.refund.entity.PaymentRefundItem;
import com.ymall.backend.payment.refund.entity.PaymentRefundStatus;
import com.ymall.backend.payment.refund.entity.PaymentRefundType;
import com.ymall.backend.payment.refund.repository.PaymentRefundRepository;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.service.ProductCacheInvalidator;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.repository.SellerProfileRepository;

@Service
@RequiredArgsConstructor
public class PaymentRefundTransactionService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final ProductRepository productRepository;
    private final ProductCacheInvalidator productCacheInvalidator;
    private final SellerProfileRepository sellerProfileRepository;
    private final OrderOutboxService orderOutboxService;

    @Transactional
    public PaymentRefundPreparation prepareUser(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        Order order = orderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return prepareRefund(
            order,
            memberId,
            MemberRole.ROLE_USER,
            request,
            item -> true
        );
    }

    @Transactional
    public PaymentRefundPreparation prepareSeller(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        SellerProfile profile = sellerProfileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_PROFILE_NOT_FOUND));
        Order order = orderRepository.findSellerOrderByIdForUpdate(orderId, profile.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_ORDER_NOT_FOUND));
        return prepareRefund(
            order,
            memberId,
            MemberRole.ROLE_SELLER,
            request,
            item -> isOwnedBySeller(item, profile.getId())
        );
    }

    @Transactional
    public PaymentRefundPreparation prepareAdmin(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        Order order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return prepareRefund(
            order,
            memberId,
            MemberRole.ROLE_ADMIN,
            request,
            item -> true
        );
    }

    private PaymentRefundPreparation prepareRefund(
        Order order,
        Long memberId,
        MemberRole role,
        PaymentRefundRequest request,
        Predicate<OrderItem> itemAccess
    ) {
        Long orderId = order.getId();
        PaymentRefund existing = refundRepository.findByOrderIdAndIdempotencyKey(
            orderId,
            request.idempotencyKey()
        ).orElse(null);
        if (existing != null) {
            if (role == MemberRole.ROLE_SELLER
                && existing.getItems().stream()
                    .anyMatch(item -> !itemAccess.test(item.getOrderItem()))) {
                throw new BusinessException(ErrorCode.SELLER_ORDER_NOT_FOUND);
            }
            return PaymentRefundPreparation.existing(
                toResponse(existing, item -> itemAccess.test(item.getOrderItem()))
            );
        }

        validateOrderStatus(order);
        Payment payment = paymentRepository
            .findFirstByOrderIdAndResultOrderByProcessedAtDesc(orderId, PaymentResult.SUCCESS)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED));
        if (payment.getPaymentKey() == null) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED);
        }

        List<PaymentRefund> priorRefunds = refundRepository
            .findAllByOrderIdOrderByCreatedAtDesc(orderId);
        if (priorRefunds.stream().anyMatch(refund ->
            refund.getStatus() == PaymentRefundStatus.PENDING
                || refund.getStatus() == PaymentRefundStatus.UNKNOWN
        )) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_RECONCILIATION_REQUIRED);
        }

        Map<Long, Integer> pendingQuantities = pendingQuantities(priorRefunds);
        List<RefundQuantity> quantities = resolveQuantities(
            order,
            request.items(),
            pendingQuantities,
            itemAccess
        );
        BigDecimal remainingAmount = remainingAmount(order, priorRefunds);
        BigDecimal requestedAmount = quantities.stream()
            .map(quantity -> quantity.item().getUnitPrice()
                .multiply(BigDecimal.valueOf(quantity.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (requestedAmount.signum() <= 0
            || requestedAmount.compareTo(remainingAmount) > 0) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_AMOUNT_EXCEEDED);
        }

        PaymentRefund refund = new PaymentRefund(
            payment,
            order,
            request.idempotencyKey(),
            requestedAmount.compareTo(remainingAmount) == 0
                ? PaymentRefundType.FULL
                : PaymentRefundType.PARTIAL,
            request.reason().trim(),
            memberId,
            role
        );
        quantities.forEach(quantity ->
            refund.addItem(new PaymentRefundItem(quantity.item(), quantity.quantity()))
        );
        refundRepository.save(refund);

        return new PaymentRefundPreparation(
            refund.getId(),
            payment.getPaymentKey(),
            refund.getAmount(),
            refund.getReason(),
            refund.getIdempotencyKey(),
            null
        );
    }

    @Transactional
    public PaymentRefundResponse complete(Long refundId, PaymentGatewayResult gatewayResult) {
        PaymentRefund refund = refundRepository.findByIdForUpdate(refundId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_FOUND));
        if (refund.getStatus() != PaymentRefundStatus.PENDING) {
            return toResponse(refund);
        }
        Order order = orderRepository.findByIdForUpdate(refund.getOrder().getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        validateGatewayResult(refund, order, gatewayResult);

        Map<Long, Product> products = productRepository.findAllByIdForUpdate(
            refund.getItems().stream()
                .map(item -> item.getOrderItem().getProduct().getId())
                .sorted()
                .toList()
        ).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        for (PaymentRefundItem refundItem : refund.getItems()) {
            OrderItem orderItem = refundItem.getOrderItem();
            Product product = products.get(orderItem.getProduct().getId());
            if (product == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            orderItem.recordRefund(refundItem.getQuantity());
            product.increaseStock(refundItem.getQuantity());
        }
        productCacheInvalidator.evictProductDetails(products.keySet());

        boolean fullyRefunded = gatewayResult.balanceAmount().signum() == 0;
        order.applyRefund(fullyRefunded);
        refund.getPayment().synchronizeProviderStatus(gatewayResult.status());
        refund.succeed(gatewayResult.status(), gatewayResult.balanceAmount());
        orderOutboxService.save(
            OrderEventType.REFUND_COMPLETED,
            order.getId(),
            order.getMember().getId(),
            Map.of(
                "refundId", refund.getId(),
                "refundType", refund.getType().name(),
                "refundAmount", refund.getAmount()
            )
        );
        return toResponse(refund);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
        Long refundId,
        String failureCode,
        String failureMessage,
        boolean outcomeUnknown
    ) {
        refundRepository.findByIdForUpdate(refundId)
            .ifPresent(refund ->
                refund.fail(failureCode, truncate(failureMessage), outcomeUnknown)
            );
    }

    @Transactional(readOnly = true)
    public List<PaymentRefundResponse> getRefunds(Long memberId, Long orderId) {
        orderRepository.findByIdAndMemberId(orderId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return refundRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentRefundResponse> getSellerRefunds(Long memberId, Long orderId) {
        SellerProfile profile = sellerProfileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_PROFILE_NOT_FOUND));
        orderRepository.findSellerOrderById(orderId, profile.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_ORDER_NOT_FOUND));
        Predicate<PaymentRefundItem> itemAccess = refundItem ->
            isOwnedBySeller(refundItem.getOrderItem(), profile.getId());
        return refundRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId)
            .stream()
            .filter(refund -> refund.getItems().stream().anyMatch(itemAccess))
            .map(refund -> toResponse(refund, itemAccess))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentRefundResponse> getAdminRefunds(Long orderId) {
        orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return refundRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private void validateOrderStatus(Order order) {
        if (order.getStatus() != OrderStatus.PAID
            && order.getStatus() != OrderStatus.PARTIALLY_REFUNDED) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED);
        }
    }

    private Map<Long, Integer> pendingQuantities(List<PaymentRefund> refunds) {
        Map<Long, Integer> quantities = new HashMap<>();
        refunds.stream()
            .filter(refund -> refund.getStatus() == PaymentRefundStatus.PENDING
                || refund.getStatus() == PaymentRefundStatus.UNKNOWN)
            .flatMap(refund -> refund.getItems().stream())
            .forEach(item -> quantities.merge(
                item.getOrderItem().getId(),
                item.getQuantity(),
                Integer::sum
            ));
        return quantities;
    }

    private List<RefundQuantity> resolveQuantities(
        Order order,
        List<PaymentRefundItemRequest> requests,
        Map<Long, Integer> pendingQuantities,
        Predicate<OrderItem> itemAccess
    ) {
        Map<Long, OrderItem> items = order.getItems().stream()
            .collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        if (requests == null || requests.isEmpty()) {
            List<RefundQuantity> remaining = order.getItems().stream()
                .filter(itemAccess)
                .map(item -> new RefundQuantity(
                    item,
                    item.getRefundableQuantity()
                        - pendingQuantities.getOrDefault(item.getId(), 0)
                ))
                .filter(quantity -> quantity.quantity() > 0)
                .toList();
            if (remaining.isEmpty()) {
                throw new BusinessException(ErrorCode.PAYMENT_REFUND_AMOUNT_EXCEEDED);
            }
            return remaining;
        }

        Set<Long> uniqueItemIds = new HashSet<>();
        return requests.stream().map(request -> {
            if (!uniqueItemIds.add(request.orderItemId())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            OrderItem item = items.get(request.orderItemId());
            int available = item == null
                ? 0
                : item.getRefundableQuantity()
                    - pendingQuantities.getOrDefault(item.getId(), 0);
            if (item == null
                || !itemAccess.test(item)
                || item.getEffectiveFulfillmentStatus() != OrderItemFulfillmentStatus.PENDING
                || request.quantity() > available) {
                throw new BusinessException(ErrorCode.PAYMENT_REFUND_AMOUNT_EXCEEDED);
            }
            return new RefundQuantity(item, request.quantity());
        }).toList();
    }

    private BigDecimal remainingAmount(Order order, List<PaymentRefund> refunds) {
        BigDecimal refunded = refunds.stream()
            .filter(refund -> refund.getStatus() == PaymentRefundStatus.SUCCEEDED)
            .map(PaymentRefund::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = refunds.stream()
            .filter(refund -> refund.getStatus() == PaymentRefundStatus.PENDING
                || refund.getStatus() == PaymentRefundStatus.UNKNOWN)
            .map(PaymentRefund::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return order.getTotalAmount().subtract(refunded).subtract(pending);
    }

    private void validateGatewayResult(
        PaymentRefund refund,
        Order order,
        PaymentGatewayResult result
    ) {
        BigDecimal expectedBalance = order.getTotalAmount().subtract(
            order.getItems().stream()
                .map(item -> item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getRefundedQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        ).subtract(refund.getAmount());
        boolean valid = result != null
            && refund.getPayment().getPaymentKey().equals(result.paymentKey())
            && order.getPaymentOrderId().equals(result.orderId())
            && result.totalAmount() != null
            && order.getTotalAmount().compareTo(result.totalAmount()) == 0
            && result.balanceAmount() != null
            && expectedBalance.compareTo(result.balanceAmount()) == 0
            && (result.status() == PaymentGatewayStatus.CANCELED
                || result.status() == PaymentGatewayStatus.PARTIAL_CANCELED);
        if (!valid) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_PROVIDER_MISMATCH);
        }
    }

    PaymentRefundResponse toResponse(PaymentRefund refund) {
        return toResponse(refund, item -> true);
    }

    private PaymentRefundResponse toResponse(
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

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private boolean isOwnedBySeller(OrderItem item, Long sellerProfileId) {
        return item.getProduct().getSellerProfile() != null
            && item.getProduct().getSellerProfile().getId().equals(sellerProfileId);
    }

    private record RefundQuantity(OrderItem item, int quantity) {
    }
}
