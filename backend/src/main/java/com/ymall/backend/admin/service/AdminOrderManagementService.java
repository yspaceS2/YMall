package com.ymall.backend.admin.service;

import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.admin.dto.AdminOrderResponse;
import com.ymall.backend.admin.mapper.AdminMapper;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.entity.PaymentResult;
import com.ymall.backend.payment.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class AdminOrderManagementService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final AdminMapper adminMapper;
    private final AdminPageRequestFactory pageRequestFactory;

    PageResponse<AdminOrderResponse> getOrders(
        int page,
        int size,
        String keyword,
        String workType
    ) {
        String normalizedKeyword = normalize(keyword);
        Long orderId = parseOrderId(normalizedKeyword);
        Page<Order> orders = orderRepository.searchAdminOrders(
            orderId == null ? normalizedKeyword : "",
            orderId,
            "PENDING_REFUND".equals(workType),
            "PENDING_RETURN".equals(workType),
            pageRequestFactory.create(page, size)
        );
        return toOrderPage(orders);
    }

    AdminOrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findAdminOrderById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        boolean refundSupported = paymentRepository
            .existsByOrderIdAndResultAndPaymentKeyIsNotNull(orderId, PaymentResult.SUCCESS);
        return adminMapper.toOrderResponse(order, refundSupported);
    }

    private PageResponse<AdminOrderResponse> toOrderPage(Page<Order> orders) {
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Set<Long> refundSupportedOrderIds = orderIds.isEmpty()
            ? Set.of()
            : paymentRepository.findRefundSupportedOrderIds(orderIds, PaymentResult.SUCCESS);
        return PageResponse.from(orders.map(order -> adminMapper.toOrderResponse(
            order,
            refundSupportedOrderIds.contains(order.getId())
        )));
    }

    private String normalize(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private Long parseOrderId(String keyword) {
        try {
            return keyword.isBlank() ? null : Long.valueOf(keyword);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
