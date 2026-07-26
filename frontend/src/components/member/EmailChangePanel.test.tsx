import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
    changeMemberEmail,
    confirmEmailChangeReauthentication,
    requestNewEmailVerification,
    startEmailChangeReauthentication,
} from '../../api/auth'
import { EmailChangePanel } from './EmailChangePanel'

const navigate = vi.fn()
const logout = vi.fn()

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigate,
}))

vi.mock('../../auth/useAuth', () => ({
    useAuth: () => ({ logout }),
}))

vi.mock('../../api/auth', () => ({
    startEmailChangeReauthentication: vi.fn(),
    confirmEmailChangeReauthentication: vi.fn(),
    requestNewEmailVerification: vi.fn(),
    changeMemberEmail: vi.fn(),
}))

describe('EmailChangePanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        logout.mockResolvedValue(undefined)
    })

    it('일반 회원은 현재 비밀번호와 새 이메일 인증 후 로그아웃한다', async () => {
        vi.mocked(startEmailChangeReauthentication).mockResolvedValue({
            verificationRequired: false,
            requestId: null,
            maskedEmail: null,
            expiresIn: 600,
        })
        vi.mocked(requestNewEmailVerification).mockResolvedValue({
            requestId: 'new-email-request',
            expiresIn: 300,
        })
        vi.mocked(changeMemberEmail).mockResolvedValue(undefined)

        render(<EmailChangePanel currentEmail="old@example.com" hasPassword />)

        fireEvent.change(screen.getByLabelText('현재 비밀번호'), {
            target: { value: 'password123' },
        })
        fireEvent.click(screen.getByRole('button', { name: '현재 비밀번호로 본인 확인' }))
        await screen.findByLabelText('새 이메일')

        fireEvent.change(screen.getByLabelText('새 이메일'), {
            target: { value: 'NEW@Example.com' },
        })
        fireEvent.click(screen.getByRole('button', { name: '새 이메일 인증번호 발송' }))
        await screen.findByLabelText('new@example.com 인증번호')

        fireEvent.change(screen.getByLabelText('new@example.com 인증번호'), {
            target: { value: '123456' },
        })
        fireEvent.click(screen.getByRole('button', { name: '이메일 변경 완료' }))

        await waitFor(() => {
            expect(changeMemberEmail).toHaveBeenCalledWith(
                'new-email-request',
                'new@example.com',
                '123456',
            )
            expect(logout).toHaveBeenCalledOnce()
            expect(navigate).toHaveBeenCalledWith('/login', {
                replace: true,
                state: { emailChanged: true },
            })
        })
    })

    it('소셜 회원은 현재 등록 이메일 인증을 먼저 완료한다', async () => {
        vi.mocked(startEmailChangeReauthentication).mockResolvedValue({
            verificationRequired: true,
            requestId: 'current-email-request',
            maskedEmail: 's***@example.com',
            expiresIn: 300,
        })
        vi.mocked(confirmEmailChangeReauthentication).mockResolvedValue(undefined)

        render(<EmailChangePanel currentEmail="social@example.com" hasPassword={false} />)

        fireEvent.click(screen.getByRole('button', { name: '현재 이메일로 인증번호 받기' }))
        await screen.findByLabelText('s***@example.com 인증번호')

        fireEvent.change(screen.getByLabelText('s***@example.com 인증번호'), {
            target: { value: '654321' },
        })
        fireEvent.click(screen.getByRole('button', { name: '본인 확인 완료' }))

        await screen.findByLabelText('새 이메일')
        expect(confirmEmailChangeReauthentication).toHaveBeenCalledWith(
            'current-email-request',
            '654321',
        )
    })
})
