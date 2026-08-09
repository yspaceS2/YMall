export type NotificationType =
    | 'ORDER_CREATED'
    | 'PAYMENT_COMPLETED'
    | 'PAYMENT_FAILED'
    | 'ORDER_CANCELED'
    | 'ORDER_PREPARING'
    | 'ORDER_SHIPPED'
    | 'ORDER_DELIVERED'
    | 'RETURN_REQUESTED'
    | 'RETURN_APPROVED'
    | 'RETURN_REJECTED'
    | 'PRODUCT_QUESTION_CREATED'
    | 'PRODUCT_QUESTION_ANSWERED'

export interface Notification {
    notificationId: number
    type: NotificationType
    title: string
    message: string
    targetUrl: string | null
    readAt: string | null
    createdAt: string
}

export interface NotificationUnreadCount {
    unreadCount: number
}

export interface NotificationReadAllResult {
    updatedCount: number
}
