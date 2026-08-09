package com.ymall.backend.order.returnrequest.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.order.returnrequest.dto.ReturnRequestCreateRequest;
import com.ymall.backend.order.returnrequest.dto.ReturnRequestResponse;
import com.ymall.backend.order.returnrequest.entity.ProductReturnRequest;
import com.ymall.backend.order.returnrequest.entity.ReturnRequestStatus;
import com.ymall.backend.order.returnrequest.repository.ProductReturnRequestRepository;
import com.ymall.backend.notification.event.NotificationEvent;
import com.ymall.backend.notification.service.NotificationService;
import com.ymall.backend.payment.refund.entity.PaymentRefund;
import com.ymall.backend.payment.refund.repository.PaymentRefundRepository;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.service.SellerProfileService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnRequestTransactionService {

    private static final int RETURN_WINDOW_DAYS = 7;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductReturnRequestRepository returnRequestRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final OrderRepository orderRepository;
    private final SellerProfileService sellerProfileService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Transactional
    public ReturnRequestResponse create(
        Long memberId,
        Long orderId,
        ReturnRequestCreateRequest request
    ) {
        Order order = orderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        OrderItem item = order.getItems().stream()
            .filter(candidate -> candidate.getId().equals(request.orderItemId()))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        LocalDateTime deliveredAt = item.getDeliveredAt();
        LocalDateTime now = LocalDateTime.now(clock);
        if (item.getEffectiveFulfillmentStatus() != OrderItemFulfillmentStatus.DELIVERED
            || deliveredAt == null
            || now.isAfter(deliveredAt.plusDays(RETURN_WINDOW_DAYS))) {
            throw new BusinessException(ErrorCode.RETURN_REQUEST_NOT_ALLOWED);
        }

        int pendingQuantity = returnRequestRepository.sumQuantityByOrderItemIdAndStatus(
            item.getId(),
            ReturnRequestStatus.REQUESTED
        );
        int availableQuantity = item.getRefundableQuantity() - pendingQuantity;
        if (request.quantity() > availableQuantity) {
            throw new BusinessException(ErrorCode.RETURN_REQUEST_QUANTITY_EXCEEDED);
        }

        ProductReturnRequest returnRequest = returnRequestRepository.save(
            new ProductReturnRequest(
                item,
                order.getMember(),
                request.quantity(),
                request.reason().trim(),
                now
            )
        );
        Long sellerMemberId = item.getProduct().getSellerProfile().getMember().getId();
        notificationService.create(NotificationEvent.returnRequested(
            UUID.randomUUID(),
            sellerMemberId,
            returnRequest.getId(),
            item.getProductName()
        ));
        return toResponse(returnRequest);
    }

    public List<ReturnRequestResponse> getMemberRequests(Long memberId, Long orderId) {
        orderRepository.findByIdAndMemberId(orderId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return returnRequestRepository
            .findAllByOrderItemOrderIdAndMemberIdOrderByRequestedAtDesc(orderId, memberId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public PageResponse<ReturnRequestResponse> getSellerRequests(
        Long memberId,
        int page,
        int size,
        ReturnRequestStatus status,
        String keyword
    ) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        Page<ProductReturnRequest> requests = returnRequestRepository
            .searchSellerRequests(
                profile.getId(),
                status != null,
                status == null ? ReturnRequestStatus.REQUESTED : status,
                normalizeKeyword(keyword),
                PageRequest.of(
                    Math.max(page - 1, 0),
                    Math.min(Math.max(size, 1), MAX_PAGE_SIZE)
                )
            );
        return PageResponse.from(requests.map(this::toResponse));
    }

    public ReturnRequestResponse getSellerRequest(
        Long memberId,
        Long returnRequestId
    ) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        ProductReturnRequest request = returnRequestRepository
            .findByIdAndOrderItemProductSellerProfileId(
                returnRequestId,
                profile.getId()
            )
            .orElseThrow(() ->
                new BusinessException(ErrorCode.RETURN_REQUEST_NOT_FOUND)
            );
        return toResponse(request);
    }

    @Transactional
    public ReturnApprovalCommand prepareApproval(Long memberId, Long returnRequestId) {
        ProductReturnRequest request = getSellerRequestForUpdate(memberId, returnRequestId);
        validateRequested(request);
        return new ReturnApprovalCommand(
            request.getId(),
            request.getOrderItem().getOrder().getId(),
            request.getOrderItem().getId(),
            request.getQuantity(),
            request.getReason()
        );
    }

    @Transactional
    public ReturnRequestResponse completeApproval(
        Long memberId,
        Long returnRequestId,
        Long paymentRefundId,
        String sellerResponse
    ) {
        ProductReturnRequest request = getSellerRequestForUpdate(memberId, returnRequestId);
        if (request.getStatus() == ReturnRequestStatus.APPROVED) {
            return toResponse(request);
        }
        validateRequested(request);
        PaymentRefund paymentRefund = paymentRefundRepository.findById(paymentRefundId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_FOUND));
        request.approve(
            paymentRefund,
            sellerResponse.trim(),
            LocalDateTime.now(clock)
        );
        notificationService.create(NotificationEvent.returnProcessed(
            UUID.randomUUID(),
            request.getMember().getId(),
            request.getOrderItem().getOrder().getId(),
            request.getOrderItem().getProductName(),
            true
        ));
        return toResponse(request);
    }

    @Transactional
    public ReturnRequestResponse reject(
        Long memberId,
        Long returnRequestId,
        String sellerResponse
    ) {
        ProductReturnRequest request = getSellerRequestForUpdate(memberId, returnRequestId);
        validateRequested(request);
        request.reject(sellerResponse.trim(), LocalDateTime.now(clock));
        notificationService.create(NotificationEvent.returnProcessed(
            UUID.randomUUID(),
            request.getMember().getId(),
            request.getOrderItem().getOrder().getId(),
            request.getOrderItem().getProductName(),
            false
        ));
        return toResponse(request);
    }

    private ProductReturnRequest getSellerRequestForUpdate(
        Long memberId,
        Long returnRequestId
    ) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        return returnRequestRepository.findSellerRequestForUpdate(
            returnRequestId,
            profile.getId()
        ).orElseThrow(() -> new BusinessException(ErrorCode.RETURN_REQUEST_NOT_FOUND));
    }

    private void validateRequested(ProductReturnRequest request) {
        if (request.getStatus() != ReturnRequestStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.RETURN_REQUEST_STATUS_INVALID);
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "";
        }
        return keyword.trim();
    }

    private ReturnRequestResponse toResponse(ProductReturnRequest request) {
        OrderItem item = request.getOrderItem();
        Member member = request.getMember();
        return new ReturnRequestResponse(
            request.getId(),
            item.getOrder().getId(),
            item.getId(),
            item.getProduct().getId(),
            item.getProductName(),
            item.getProduct().getThumbnailUrl(),
            member.getName(),
            request.getQuantity(),
            request.getReason(),
            request.getStatus(),
            request.getSellerResponse(),
            request.getPaymentRefund() == null ? null : request.getPaymentRefund().getId(),
            item.getDeliveredAt() == null
                ? null
                : item.getDeliveredAt().plusDays(RETURN_WINDOW_DAYS),
            request.getRequestedAt(),
            request.getProcessedAt()
        );
    }
}
