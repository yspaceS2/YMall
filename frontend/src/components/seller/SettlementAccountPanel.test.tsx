import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
    getSellerSettlementAccount,
    upsertSellerSettlementAccount,
} from '../../api/seller'
import { ApiError } from '../../api/client'
import { SettlementAccountPanel } from './SettlementAccountPanel'

vi.mock('../../api/seller', () => ({
    getSellerSettlementAccount: vi.fn(),
    upsertSellerSettlementAccount: vi.fn(),
}))

const savedAccount = {
    settlementAccountId: 1,
    bankCode: '004',
    bankName: 'KB국민은행',
    accountHolder: '테스트판매자',
    maskedAccountNumber: '****0001',
    verificationStatus: 'UNVERIFIED' as const,
    verifiedAt: null,
    updatedAt: '2026-07-27T01:00:00',
}

describe('SettlementAccountPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('등록된 계좌는 마스킹해서 표시하고 원문 계좌번호를 채우지 않는다', async () => {
        vi.mocked(getSellerSettlementAccount).mockResolvedValue(savedAccount)

        render(<SettlementAccountPanel />)

        expect(await screen.findByText('KB국민은행 ****0001')).toBeInTheDocument()
        expect(screen.getByText('예금주 테스트판매자')).toBeInTheDocument()
        expect(screen.getByLabelText('새 계좌번호')).toHaveValue('')
        expect(screen.queryByText('000000000001')).not.toBeInTheDocument()
    })

    it('계좌가 없으면 등록 폼을 표시하고 숫자만 서버에 전달한다', async () => {
        const currentPassword = crypto.randomUUID()
        vi.mocked(getSellerSettlementAccount).mockRejectedValue(
            new ApiError('등록된 정산 계좌가 없습니다.', 404, 'SELLER_SETTLEMENT_ACCOUNT_NOT_FOUND'),
        )
        vi.mocked(upsertSellerSettlementAccount).mockResolvedValue(savedAccount)

        render(<SettlementAccountPanel />)

        const accountNumber = await screen.findByLabelText('계좌번호')
        fireEvent.change(screen.getByLabelText('예금주'), {
            target: { value: '테스트판매자' },
        })
        fireEvent.change(accountNumber, {
            target: { value: '0000-0000-0001' },
        })
        fireEvent.change(screen.getByLabelText('현재 비밀번호'), {
            target: { value: currentPassword },
        })
        fireEvent.click(screen.getByRole('button', { name: '정산 계좌 등록' }))

        await waitFor(() => {
            expect(upsertSellerSettlementAccount).toHaveBeenCalledWith({
                bankCode: '004',
                accountHolder: '테스트판매자',
                accountNumber: '000000000001',
                currentPassword,
            })
        })
        expect(await screen.findByText('정산 계좌 정보가 등록되었습니다.')).toBeInTheDocument()
        expect(screen.getByLabelText('새 계좌번호')).toHaveValue('')
        expect(screen.getByLabelText('현재 비밀번호')).toHaveValue('')
    })
})
