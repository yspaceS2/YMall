import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
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
        render(<AdminSellerApplicationPanel />)

        expect(await screen.findByText('YMall Store')).toBeInTheDocument()
        await user.click(screen.getByRole('button', { name: '승인' }))

        await waitFor(() => {
            expect(mocks.reviewSellerApplication).toHaveBeenCalledWith(1, 'APPROVED', undefined)
        })
        expect(await screen.findByText("'YMall Store' 판매자 신청을 승인했습니다."))
            .toBeInTheDocument()
        expect(screen.queryByText('member@example.com')).not.toBeInTheDocument()
    })
})
