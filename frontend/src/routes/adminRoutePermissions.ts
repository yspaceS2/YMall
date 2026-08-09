import type { AdminPermission } from '../types/admin'

/** 관리자 Route와 진입에 필요한 최소 권한의 단일 기준이다. Backend 권한 정책을 대체하지 않는다. */
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
