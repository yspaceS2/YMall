import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { SellerApplicationPanel } from './SellerApplicationPanel'

const mocks = vi.hoisted(() => ({
    getMySellerApplication: vi.fn(),
    createSellerApplication: vi.fn(),
    logout: vi.fn(),
    role: 'ROLE_USER',
}))

vi.mock('../../api/sellerApplications', () => ({
    getMySellerApplication: mocks.getMySellerApplication,
    createSellerApplication: mocks.createSellerApplication,
}))

vi.mock('../../auth/useAuth', () => ({
    useAuth: () => ({ logout: mocks.logout, role: mocks.role }),
}))

describe('SellerApplicationPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.role = 'ROLE_USER'
        mocks.getMySellerApplication.mockRejectedValue(
            new ApiError('신청 없음', 404, 'SELLER_APPLICATION_NOT_FOUND'),
        )
        mocks.createSellerApplication.mockResolvedValue({
            sellerApplicationId: 1,
            memberId: 10,
            memberName: '테스트 회원',
            memberEmail: 'member@example.com',
            storeName: 'YMall Store',
            businessNumber: '123-45-67890',
            description: '상점 소개',
            status: 'PENDING',
            rejectionReason: null,
            reviewedAt: null,
            createdAt: '2026-07-28T12:00:00',
            updatedAt: '2026-07-28T12:00:00',
        })
    })

    it('신청 내역이 없는 회원이 판매자 신청을 접수한다', async () => {
        const user = userEvent.setup()
        render(<SellerApplicationPanel />)

        await user.type(await screen.findByLabelText('상점명'), 'YMall Store')
        await user.type(screen.getByLabelText('사업자등록번호'), '123-45-67890')
        await user.type(screen.getByLabelText('상점 소개'), '상점 소개')
        await user.click(screen.getByRole('button', { name: '판매자 신청' }))

        await waitFor(() => {
            expect(mocks.createSellerApplication).toHaveBeenCalledWith({
                storeName: 'YMall Store',
                businessNumber: '123-45-67890',
                description: '상점 소개',
            })
        })
        expect(await screen.findByText('신청이 접수되어 관리자 심사를 기다리고 있습니다.'))
            .toBeInTheDocument()
    })

    it('판매자로 재로그인하면 판매자 센터로 이동한다', async () => {
        mocks.role = 'ROLE_SELLER'

        render(
            <MemoryRouter initialEntries={['/mypage/seller-application']}>
                <Routes>
                    <Route
                        path="/mypage/seller-application"
                        element={<SellerApplicationPanel />}
                    />
                    <Route path="/seller" element={<p>판매자 센터</p>} />
                </Routes>
            </MemoryRouter>,
        )

        expect(await screen.findByText('판매자 센터')).toBeInTheDocument()
        expect(mocks.getMySellerApplication).not.toHaveBeenCalled()
    })
})
