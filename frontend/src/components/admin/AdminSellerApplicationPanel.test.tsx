import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AdminAuthorizationContext } from '../../auth/AdminAuthorizationContext'
import { AdminSellerApplicationPanel } from './AdminSellerApplicationPanel'

const mocks = vi.hoisted(() => ({
    getAdminSellerApplications: vi.fn(),
    reviewSellerApplication: vi.fn(),
}))

vi.mock('../../api/sellerApplications', () => ({
    getAdminSellerApplications: mocks.getAdminSellerApplications,
    reviewSellerApplication: mocks.reviewSellerApplication,
}))

const application = {
    sellerApplicationId: 1,
    memberId: 10,
    memberName: '테스트 회원',
    memberEmail: 'member@example.com',
    storeName: 'YMall Store',
    businessNumber: '123-45-67890',
    description: '상점 소개',
    status: 'PENDING' as const,
    rejectionReason: null,
    reviewedAt: null,
    createdAt: '2026-07-28T12:00:00',
    updatedAt: '2026-07-28T12:00:00',
}

describe('AdminSellerApplicationPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.getAdminSellerApplications.mockResolvedValue({
            content: [application],
            page: 1,
            size: 50,
            totalElements: 1,
            totalPages: 1,
            hasNext: false,
            hasPrevious: false,
        })
        mocks.reviewSellerApplication.mockResolvedValue({
            ...application,
            status: 'APPROVED',
        })
    })

    it('관리자가 대기 중인 판매자 신청을 승인한다', async () => {
        const user = userEvent.setup()
        render(
            <AdminAuthorizationContext.Provider value={{
                authorization: {
                    memberId: 1,
                    adminGrade: 'SUPERVISOR',
                    permissions: ['SELLER_APPLICATION_REVIEW', 'SELLER_APPLICATION_DECIDE'],
                },
                hasPermission: (...permissions) => permissions.includes('SELLER_APPLICATION_DECIDE'),
            }}>
                <AdminSellerApplicationPanel />
            </AdminAuthorizationContext.Provider>,
        )

        expect(await screen.findByText('YMall Store')).toBeInTheDocument()
        await user.click(screen.getByRole('button', { name: '승인' }))

        await waitFor(() => {
            expect(mocks.reviewSellerApplication).toHaveBeenCalledWith(1, 'APPROVED', undefined)
        })
        expect(await screen.findByText("'YMall Store' 판매자 신청을 승인했습니다."))
            .toBeInTheDocument()
        expect(screen.queryByText('member@example.com')).not.toBeInTheDocument()
    })

    it('매니저는 승인·반려 없이 보완 요청만 처리한다', async () => {
        const user = userEvent.setup()
        render(
            <AdminAuthorizationContext.Provider value={{
                authorization: {
                    memberId: 2,
                    adminGrade: 'MANAGER',
                    permissions: ['SELLER_APPLICATION_REVIEW'],
                },
                hasPermission: (...permissions) => permissions.includes('SELLER_APPLICATION_REVIEW'),
            }}>
                <AdminSellerApplicationPanel />
            </AdminAuthorizationContext.Provider>,
        )

        expect(await screen.findByText('YMall Store')).toBeInTheDocument()
        expect(screen.queryByRole('button', { name: '승인' })).not.toBeInTheDocument()
        expect(screen.queryByRole('button', { name: '반려' })).not.toBeInTheDocument()
        await user.type(screen.getByLabelText('보완 요청·반려 사유'), '서류 설명을 보완해 주세요.')
        await user.click(screen.getByRole('button', { name: '보완 요청' }))

        await waitFor(() => {
            expect(mocks.reviewSellerApplication).toHaveBeenCalledWith(
                1,
                'NEEDS_REVISION',
                '서류 설명을 보완해 주세요.',
            )
        })
    })
})
