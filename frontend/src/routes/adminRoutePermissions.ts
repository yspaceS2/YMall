import type { AdminPermission } from '../types/admin'

export const ADMIN_ROUTE_PERMISSIONS = {
    dashboard: ['DASHBOARD_READ'],
    members: ['MEMBER_READ'],
    sellers: ['SELLER_READ'],
    sellerApplications: ['SELLER_APPLICATION_REVIEW'],
    categoryRead: ['CATEGORY_READ'],
    categoryManage: ['CATEGORY_MANAGE_ALL'],
    productReview: ['PRODUCT_REVIEW'],
    orders: ['REFUND_STANDARD'],
    support: ['SUPPORT_REPLY'],
    settlement: ['SETTLEMENT_REVIEW'],
} as const satisfies Record<string, readonly AdminPermission[]>
