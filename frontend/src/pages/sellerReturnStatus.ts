import type { ReturnRequestStatus } from '../types/order'

export const returnStatusLabel: Record<ReturnRequestStatus, string> = {
    REQUESTED: '처리 대기',
    APPROVED: '승인·환불 완료',
    REJECTED: '거절',
}
