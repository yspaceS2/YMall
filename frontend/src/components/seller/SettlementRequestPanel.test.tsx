import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'

import {
    createSettlementRequest,
    getSettlementAvailability,
    getSettlementRequests,
} from '../../api/seller'
import { SettlementRequestPanel } from './SettlementRequestPanel'

vi.mock('../../api/seller', () => ({
    createSettlementRequest: vi.fn(),
    getSettlementAvailability: vi.fn(),
    getSettlementRequests: vi.fn(),
}))

const availability = {
    entryCount: 1,
    grossAmount: 10000,
    feeAmount: 300,
    settlementAmount: 9700,
    hasSettlementAccount: true,
    canRequest: true,
}

describe('SettlementRequestPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getSettlementAvailability).mockResolvedValue(availability)
        vi.mocked(getSettlementRequests).mockResolvedValue({
            content: [],
            page: 1,
            size: 24,
            totalElements: 0,
            totalPages: 0,
            hasNext: false,
            hasPrevious: false,
        })
        vi.mocked(createSettlementRequest).mockResolvedValue({
            settlementRequestId: 1,
            sellerProfileId: 1,
            storeName: '테스트 상점',
            periodStart: null,
            periodEnd: null,
            status: 'REQUESTED',
            grossAmount: 10000,
            feeAmount: 300,
            settlementAmount: 9700,
            rejectionReason: null,
            mockPaymentReference: null,
            reviewedAt: null,
            paidAt: null,
            createdAt: '2026-07-01T00:00:00',
            updatedAt: '2026-07-01T00:00:00',
        })
    })

    it('선택된 정산 화면만 표시한다', async () => {
        const { rerender } = render(
            <MemoryRouter>
                <SettlementRequestPanel view="request" />
            </MemoryRouter>,
        )

        expect(await screen.findByRole('button', {
            name: '9,700원 정산 신청',
        })).toBeVisible()
        expect(screen.queryByText('정산 신청 이력이 없습니다.')).not.toBeInTheDocument()

        rerender(
            <MemoryRouter>
                <SettlementRequestPanel view="history" />
            </MemoryRouter>,
        )
        expect(await screen.findByText('조건에 맞는 정산 신청이 없습니다.')).toBeVisible()
        expect(screen.queryByRole('button', {
            name: '9,700원 정산 신청',
        })).not.toBeInTheDocument()
    })

    it('월 선택 없이 현재 정산 가능 금액 전액을 신청한다', async () => {
        const user = userEvent.setup()
        render(
            <MemoryRouter>
                <SettlementRequestPanel view="request" />
            </MemoryRouter>,
        )
        await user.click(await screen.findByRole('button', {
            name: '9,700원 정산 신청',
        }))

        await waitFor(() => {
            expect(createSettlementRequest).toHaveBeenCalledWith()
        })
        expect(getSettlementAvailability).toHaveBeenCalledWith(expect.any(AbortSignal))
        expect(await screen.findByText('정산을 신청했습니다.')).toBeInTheDocument()
    })
})
