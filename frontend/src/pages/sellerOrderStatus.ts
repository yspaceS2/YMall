import type { FulfillmentStatus } from '../types/seller'

export const statusLabels: Record<FulfillmentStatus, string> = {
    PENDING: '처리 대기',
    PREPARING: '상품 준비 중',
    SHIPPED: '배송 중',
    DELIVERED: '배송 완료',
}
