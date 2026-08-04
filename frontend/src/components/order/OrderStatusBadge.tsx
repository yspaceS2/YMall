import type { OrderStatus } from '../../types/order'
import { getOrderStatusLabel } from '../../utils/order'
import { StatusBadge, type StatusBadgeTone } from '../ui/StatusBadge'

const orderStatusTones: Record<OrderStatus, StatusBadgeTone> = {
    PENDING_PAYMENT: 'warning',
    PAID: 'info',
    PAYMENT_FAILED: 'danger',
    CANCELED: 'neutral',
    PARTIALLY_REFUNDED: 'warning',
    REFUNDED: 'neutral',
    PREPARING: 'info',
    SHIPPED: 'info',
    DELIVERED: 'success',
}

export function OrderStatusBadge({ status, className = '' }: { status: OrderStatus; className?: string }) {
    return (
        <StatusBadge className={className} tone={orderStatusTones[status]}>
            {getOrderStatusLabel(status)}
        </StatusBadge>
    )
}
