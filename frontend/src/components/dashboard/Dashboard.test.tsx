import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AdminAuthorizationContext } from '../../auth/AdminAuthorizationContext'
import {
    getAdminDashboardStatistics,
    getSellerDashboardStatistics,
} from '../../api/dashboard'
import type {
    AdminDashboardStatistics,
    SellerDashboardStatistics,
} from '../../types/dashboard'
import type { AdminPermission } from '../../types/admin'
import { AdminDashboard } from './AdminDashboard'
import { SellerDashboard } from './SellerDashboard'

vi.mock('../../api/dashboard', () => ({
    getAdminDashboardStatistics: vi.fn(),
    getSellerDashboardStatistics: vi.fn(),
}))

const sellerStatistics: SellerDashboardStatistics = {
    period: { period: '30d', from: '2026-07-04', to: '2026-08-02', interval: 'DAY' },
    netSalesAmount: 320000,
    orderCount: 12,
    salesQuantity: 18,
    trend: [
        { date: '2026-08-01', netSalesAmount: 120000, orderCount: 5, salesQuantity: 7 },
        { date: '2026-08-02', netSalesAmount: 200000, orderCount: 7, salesQuantity: 11 },
    ],
    orderStatusCounts: [
        { status: 'PAID', count: 7 },
        { status: 'DELIVERED', count: 5 },
    ],
    topProducts: [
        { productId: 1, productName: '테스트 상품', salesQuantity: 9, netSalesAmount: 180000 },
    ],
    settlement: { availableAmount: 150000, processingAmount: 70000, completedAmount: 100000 },
    pendingTasks: { orders: 3, returns: 1, questions: 2 },
    generatedAt: '2026-08-02T14:00:00+09:00',
}

const adminStatistics: AdminDashboardStatistics = {
    period: { period: '30d', from: '2026-07-04', to: '2026-08-02', interval: 'DAY' },
    netTransactionAmount: 840000,
    orderCount: 31,
    salesQuantity: 46,
    transactionTrend: sellerStatistics.trend,
    registrationTrend: [
        { date: '2026-08-01', members: 8, sellers: 2 },
        { date: '2026-08-02', members: 12, sellers: 3 },
    ],
    categorySales: [
        { categoryId: 2, categoryName: '패션', netSalesAmount: 480000, salesQuantity: 24 },
        { categoryId: 3, categoryName: '식품', netSalesAmount: 180000, salesQuantity: 18 },
        { categoryId: 4, categoryName: '가전', netSalesAmount: 90000, salesQuantity: 4 },
        { categoryId: 5, categoryName: '생활', netSalesAmount: 70000, salesQuantity: 7 },
        { categoryId: 6, categoryName: '뷰티', netSalesAmount: 50000, salesQuantity: 5 },
        { categoryId: 7, categoryName: '취미', netSalesAmount: 30000, salesQuantity: 3 },
        { categoryId: 8, categoryName: '자동차', netSalesAmount: 20000, salesQuantity: 2 },
        { categoryId: 9, categoryName: '가구', netSalesAmount: 0, salesQuantity: 0 },
    ],
    topProducts: sellerStatistics.topProducts,
    pendingTasks: { products: 4, sellers: 2, refunds: 1, returns: 3, settlements: 2, support: 5 },
    generatedAt: '2026-08-02T14:00:00+09:00',
}

const adminPermissions: AdminPermission[] = [
    'DASHBOARD_READ',
    'PRODUCT_REVIEW',
    'SELLER_APPLICATION_DECIDE',
    'REFUND_STANDARD',
    'SETTLEMENT_APPROVE',
    'SUPPORT_REPLY',
]

const renderAdminDashboard = (permissions: AdminPermission[] = adminPermissions) => render(
    <AdminAuthorizationContext.Provider value={{
        authorization: {
            memberId: 1,
            adminGrade: 'SUPER_ADMIN',
            permissions,
        },
        hasPermission: (...requiredPermissions) => requiredPermissions.some((permission) => permissions.includes(permission)),
    }}>
        <MemoryRouter><AdminDashboard /></MemoryRouter>
    </AdminAuthorizationContext.Provider>,
)

describe('dashboard visualization', () => {
    beforeEach(() => {
        vi.mocked(getSellerDashboardStatistics).mockResolvedValue(sellerStatistics)
        vi.mocked(getAdminDashboardStatistics).mockResolvedValue(adminStatistics)
    })

    it('판매자 순매출과 처리 업무를 시각화하고 기간을 변경한다', async () => {
        render(<MemoryRouter><SellerDashboard /></MemoryRouter>)

        expect(await screen.findByText('320,000원')).toBeInTheDocument()
        expect(screen.getByRole('img', { name: '기간별 순매출 추이' })).toBeInTheDocument()
        expect(screen.getByText('테스트 상품')).toBeInTheDocument()
        expect(screen.getByText('정산 가능')).toBeInTheDocument()
        fireEvent.pointerEnter(screen.getByLabelText('8.01 · 순매출 120,000원 · 주문 5건 · 판매 7개'))
        expect(screen.getByRole('tooltip')).toHaveTextContent('8.01 · 순매출 120,000원 · 주문 5건 · 판매 7개')
        expect(screen.getByRole('link', { name: '주문 3건 관리 페이지로 이동' })).toHaveAttribute('href', '/seller/orders?workType=ACTION_REQUIRED')
        expect(screen.getByRole('link', { name: '정산 가능 150,000원 정산 관리 페이지로 이동' })).toHaveAttribute('href', '/seller/settlement?tab=request')
        expect(screen.getByRole('link', { name: '처리 중 70,000원 정산 관리 페이지로 이동' })).toHaveAttribute('href', '/seller/settlement?tab=history&workType=PROCESSING')

        fireEvent.click(screen.getByRole('button', { name: '6개월' }))
        await waitFor(() => {
            expect(getSellerDashboardStatistics).toHaveBeenLastCalledWith('6m', expect.any(AbortSignal))
        })
    })

    it('관리자 거래·가입·대기 업무를 시각화한다', async () => {
        renderAdminDashboard()

        expect(await screen.findByText('840,000원')).toBeInTheDocument()
        expect(screen.getByText('신규 회원·판매자')).toBeInTheDocument()
        expect(screen.getByText('패션')).toBeInTheDocument()
        expect(screen.getByText('가구')).toBeInTheDocument()
        expect(screen.getByText('0원')).toBeInTheDocument()
        expect(screen.queryByText('기타')).not.toBeInTheDocument()
        expect(screen.getByText('상품 승인')).toBeInTheDocument()
        expect(screen.getByText('정산 처리')).toBeInTheDocument()
        fireEvent.pointerEnter(screen.getByLabelText('8.1 · 회원 8명 · 판매자 2명'))
        expect(screen.getByRole('tooltip')).toHaveTextContent('8.1 · 회원 8명 · 판매자 2명')
        expect(screen.getByRole('link', { name: '상품 승인 4건 관리 페이지로 이동' })).toHaveAttribute('href', '/admin/products')
        expect(screen.getByRole('link', { name: '환불 처리 1건 관리 페이지로 이동' })).toHaveAttribute('href', '/admin/orders?workType=PENDING_REFUND')
        expect(screen.getByRole('link', { name: '반품 처리 3건 관리 페이지로 이동' })).toHaveAttribute('href', '/admin/orders?workType=PENDING_RETURN')
        expect(screen.getByRole('link', { name: '정산 처리 2건 관리 페이지로 이동' })).toHaveAttribute('href', '/admin/settlement?workType=ACTION_REQUIRED')
        expect(screen.getByRole('link', { name: '고객센터 문의 5건 관리 페이지로 이동' })).toHaveAttribute('href', '/admin/support?status=WAITING')
    })

    it('현재 관리자가 처리할 권한이 있는 업무만 표시한다', async () => {
        renderAdminDashboard(['DASHBOARD_READ', 'PRODUCT_REVIEW'])

        expect(await screen.findByText('상품 승인')).toBeInTheDocument()
        expect(screen.queryByText('판매자 승인')).not.toBeInTheDocument()
        expect(screen.queryByText('정산 처리')).not.toBeInTheDocument()
        expect(screen.queryByText('고객센터 문의')).not.toBeInTheDocument()
    })
})
