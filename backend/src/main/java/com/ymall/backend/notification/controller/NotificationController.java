package com.ymall.backend.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.notification.dto.NotificationReadAllResponse;
import com.ymall.backend.notification.dto.NotificationResponse;
import com.ymall.backend.notification.dto.NotificationUnreadCountResponse;
import com.ymall.backend.notification.service.NotificationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getNotifications(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
            notificationService.getNotifications(principal.memberId(), page, size)
        );
    }

    @GetMapping("/unread-count")
    public ApiResponse<NotificationUnreadCountResponse> getUnreadCount(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ApiResponse.success(notificationService.getUnreadCount(principal.memberId()));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long notificationId
    ) {
        return ApiResponse.success(
            notificationService.markAsRead(principal.memberId(), notificationId),
            "알림을 읽음 처리했습니다."
        );
    }

    @PatchMapping("/read-all")
    public ApiResponse<NotificationReadAllResponse> markAllAsRead(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ApiResponse.success(
            notificationService.markAllAsRead(principal.memberId()),
            "모든 알림을 읽음 처리했습니다."
        );
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long notificationId
    ) {
        notificationService.delete(principal.memberId(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        notificationService.deleteAll(principal.memberId());
        return ResponseEntity.noContent().build();
    }
}
