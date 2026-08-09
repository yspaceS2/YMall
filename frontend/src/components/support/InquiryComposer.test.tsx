import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { InquiryComposer } from './InquiryComposer'

const defaultProps = {
    admin: false,
    connected: true,
    status: 'WAITING' as const,
    message: '',
    attachments: [],
    resolution: '',
    submitting: false,
    onMessageChange: vi.fn(),
    onAttachmentsChange: vi.fn(),
    onResolutionChange: vi.fn(),
    onSubmitMessage: vi.fn(),
    onClose: vi.fn(),
}

describe('InquiryComposer', () => {
    it('실시간 상담 응답 대기 중에는 메시지 입력을 숨긴다', () => {
        render(<InquiryComposer {...defaultProps} status="LIVE_REQUESTED" />)

        expect(screen.queryByRole('button', { name: '메시지 전송' })).not.toBeInTheDocument()
        expect(screen.queryByLabelText('파일 첨부')).not.toBeInTheDocument()
    })

    it('일반 문의 상태에서는 메시지와 파일 입력을 제공한다', () => {
        render(<InquiryComposer {...defaultProps} />)

        expect(screen.getByRole('button', { name: '메시지 전송' })).toBeDisabled()
        expect(screen.getByLabelText('파일 첨부')).toBeInTheDocument()
        expect(screen.getByPlaceholderText('추가 문의를 입력해 주세요')).toBeInTheDocument()
    })
})
