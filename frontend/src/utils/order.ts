import type { OrderStatus } from '../types/order'

const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
    PENDING_PAYMENT: '결제 대기',
    PAID: '결제 완료',
    PAYMENT_FAILED: '결제 실패',
    CANCELED: '주문 취소',
    PREPARING: '상품 준비 중',
    SHIPPED: '배송 중',
    DELIVERED: '배송 완료',
}

export function getOrderStatusLabel(status: OrderStatus) {
    return ORDER_STATUS_LABELS[status]
}

export function formatOrderDate(value: string) {
    return new Intl.DateTimeFormat('ko-KR', {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(new Date(value))
}
