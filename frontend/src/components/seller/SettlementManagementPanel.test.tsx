import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'

import { SettlementManagementPanel } from './SettlementManagementPanel'

vi.mock('./SettlementAccountPanel', () => ({
    SettlementAccountPanel: () => <div>계좌 화면</div>,
}))

vi.mock('./SettlementRequestPanel', () => ({
    SettlementRequestPanel: ({ view }: { view: 'request' | 'history' }) => (
        <div>{view === 'request' ? '신청 화면' : '이력 화면'}</div>
    ),
}))

describe('SettlementManagementPanel', () => {
    it('세 메뉴를 한 행에 표시하고 선택한 내용만 보여준다', () => {
        render(
            <MemoryRouter>
                <SettlementManagementPanel />
            </MemoryRouter>,
        )

        const accountTab = screen.getByRole('tab', { name: '정산 계좌' })
        const requestTab = screen.getByRole('tab', { name: '정산 신청' })
        const historyTab = screen.getByRole('tab', { name: '신청 이력' })

        expect(accountTab).toHaveAttribute('aria-selected', 'true')
        expect(screen.getByText('계좌 화면')).toBeInTheDocument()

        fireEvent.click(requestTab)
        expect(requestTab).toHaveAttribute('aria-selected', 'true')
        expect(screen.getByText('신청 화면')).toBeInTheDocument()
        expect(screen.queryByText('계좌 화면')).not.toBeInTheDocument()

        fireEvent.click(historyTab)
        expect(historyTab).toHaveAttribute('aria-selected', 'true')
        expect(screen.getByText('이력 화면')).toBeInTheDocument()
        expect(screen.queryByText('신청 화면')).not.toBeInTheDocument()
    })

    it('URL의 탭 필터에 맞는 정산 화면을 바로 표시한다', () => {
        render(
            <MemoryRouter initialEntries={['/seller/settlement?tab=history&status=PAID']}>
                <SettlementManagementPanel />
            </MemoryRouter>,
        )

        expect(screen.getByRole('tab', { name: '신청 이력' })).toHaveAttribute('aria-selected', 'true')
        expect(screen.getByText('이력 화면')).toBeInTheDocument()
    })
})
