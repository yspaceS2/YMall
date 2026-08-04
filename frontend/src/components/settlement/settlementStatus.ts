import type { SettlementRequestStatus } from '../../types/seller'
import type { StatusBadgeTone } from '../ui/StatusBadge'

export const settlementStatusLabel: Record<SettlementRequestStatus, string> = {
    REQUESTED: '승인 대기',
    APPROVED: '지급 대기',
    REJECTED: '반려',
    PAID: '모의 지급 완료',
}

export const settlementStatuses = Object.keys(
    settlementStatusLabel,
) as SettlementRequestStatus[]

export const settlementStatusTone: Record<SettlementRequestStatus, StatusBadgeTone> = {
    REQUESTED: 'warning',
    APPROVED: 'info',
    REJECTED: 'danger',
    PAID: 'success',
}
