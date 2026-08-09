package com.ymall.backend.dashboard.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ymall.backend.order.repository.OrderItemRepository;
import com.ymall.backend.realtime.dto.RealtimeEvent;
import com.ymall.backend.realtime.service.RealtimePublisher;

@ExtendWith(MockitoExtension.class)
class DashboardRealtimePublisherTest {

    @Mock
    private RealtimePublisher realtimePublisher;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private DashboardRealtimePublisher dashboardRealtimePublisher;

    @Test
    void orderChangeInvalidatesAdminsAndEveryAffectedSeller() {
        given(orderItemRepository.findDistinctSellerMemberIdsByOrderId(10L))
            .willReturn(List.of(21L, 22L));

        dashboardRealtimePublisher.invalidateOrder(10L);

        ArgumentCaptor<RealtimeEvent> eventCaptor = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtimePublisher).publishToAdmins(eventCaptor.capture());
        verify(realtimePublisher).publishToMember(eq(21L), eventCaptor.capture());
        verify(realtimePublisher).publishToMember(eq(22L), eventCaptor.capture());
        eventCaptor.getAllValues().forEach(event -> {
            org.assertj.core.api.Assertions.assertThat(event.type())
                .isEqualTo("DASHBOARD_INVALIDATED");
            org.assertj.core.api.Assertions.assertThat(event.resource()).isEqualTo("order");
            org.assertj.core.api.Assertions.assertThat(event.resourceId()).isEqualTo(10L);
        });
    }
}
