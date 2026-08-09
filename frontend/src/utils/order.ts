import type { OrderItemFulfillmentStatus, OrderStatus } from '../types/order'
import { formatKoreanDateTime } from './dateTime'

const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
    PENDING_PAYMENT: '결제 대기',
    PAID: '결제 완료',
    PAYMENT_FAILED: '결제 실패',
    CANCELED: '주문 취소',
    PARTIALLY_REFUNDED: '부분 환불',
    REFUNDED: '환불 완료',
    PREPARING: '상품 준비 중',
    SHIPPED: '배송 중',
    DELIVERED: '배송 완료',
}

const ORDER_ITEM_FULFILLMENT_STATUS_LABELS: Record<OrderItemFulfillmentStatus, string> = {
    PENDING: '처리 대기',
    PREPARING: '상품 준비 중',
    SHIPPED: '배송 중',
    DELIVERED: '배송 완료',
}

export function getOrderStatusLabel(status: OrderStatus) {
    return ORDER_STATUS_LABELS[status]
}

export function getOrderItemFulfillmentStatusLabel(status: OrderItemFulfillmentStatus) {
    return ORDER_ITEM_FULFILLMENT_STATUS_LABELS[status]
}

export function formatOrderDate(value: string) {
    return formatKoreanDateTime(value)
}
