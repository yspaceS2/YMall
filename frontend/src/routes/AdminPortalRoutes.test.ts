import { describe, expect, it } from 'vitest'
import { ADMIN_ROUTE_PERMISSIONS } from './adminRoutePermissions'

describe('관리자 포털 라우트 권한', () => {
    it('관리 화면별 권한 매트릭스를 유지한다', () => {
        expect(ADMIN_ROUTE_PERMISSIONS).toEqual({
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
        })
    })
})
