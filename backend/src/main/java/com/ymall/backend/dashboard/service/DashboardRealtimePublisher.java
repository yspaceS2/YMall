package com.ymall.backend.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.ymall.backend.order.repository.OrderItemRepository;
import com.ymall.backend.realtime.dto.RealtimeEvent;
import com.ymall.backend.realtime.service.RealtimePublisher;

@Component
@RequiredArgsConstructor
public class DashboardRealtimePublisher {

    private final RealtimePublisher realtimePublisher;
    private final OrderItemRepository orderItemRepository;

    public void invalidateAdmins(String resource, Long resourceId) {
        realtimePublisher.publishToAdmins(event(resource, resourceId));
    }

    public void invalidateSeller(Long sellerMemberId, String resource, Long resourceId) {
        realtimePublisher.publishToMember(sellerMemberId, event(resource, resourceId));
    }

    public void invalidateSellerAndAdmins(
        Long sellerMemberId,
        String resource,
        Long resourceId
    ) {
        invalidateAdmins(resource, resourceId);
        invalidateSeller(sellerMemberId, resource, resourceId);
    }

    public void invalidateOrder(Long orderId) {
        invalidateAdmins("order", orderId);
        orderItemRepository.findDistinctSellerMemberIdsByOrderId(orderId)
            .forEach(memberId -> invalidateSeller(memberId, "order", orderId));
    }

    private RealtimeEvent event(String resource, Long resourceId) {
        return RealtimeEvent.of("DASHBOARD_INVALIDATED", resource, resourceId);
    }
}
