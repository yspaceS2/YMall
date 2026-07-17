import type { PageResponse } from '../types/api'
import type {
    Notification,
    NotificationReadAllResult,
    NotificationUnreadCount,
} from '../types/notification'
import { apiRequest } from './client'

export const NOTIFICATIONS_CHANGED_EVENT = 'ymall:notifications-changed'

export function getNotifications(page = 1, size = 20, signal?: AbortSignal) {
    return apiRequest<PageResponse<Notification>>(
        `/notifications?page=${page}&size=${size}`,
        { signal },
    )
}

export function getUnreadNotificationCount(signal?: AbortSignal) {
    return apiRequest<NotificationUnreadCount>('/notifications/unread-count', { signal })
}

export function markNotificationAsRead(notificationId: number) {
    return apiRequest<Notification>(`/notifications/${notificationId}/read`, {
        method: 'PATCH',
    })
}

export function markAllNotificationsAsRead() {
    return apiRequest<NotificationReadAllResult>('/notifications/read-all', {
        method: 'PATCH',
    })
}

export function notifyNotificationsChanged() {
    window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED_EVENT))
}
