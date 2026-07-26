import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
    changeMemberEmail,
    requestNewEmailVerification,
    startEmailChangeOAuthReauthentication,
    startEmailChangeReauthentication
} from '../../api/auth'
import { EmailChangePanel } from './EmailChangePanel'

const navigate = vi.fn()
const logout = vi.fn()
let locationState: Record<string, unknown> | null = null

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigate,
    useLocation: () => ({ pathname: '/mypage', state: locationState }),
}))

vi.mock('../../auth/useAuth', () => ({
    useAuth: () => ({ logout }),
}))

vi.mock('../../api/auth', () => ({
    startEmailChangeReauthentication: vi.fn(),
    startEmailChangeOAuthReauthentication: vi.fn(),
    getOAuthAuthorizationUrl: vi.fn(),
    requestNewEmailVerification: vi.fn(),
    changeMemberEmail: vi.fn(),
}))

describe('EmailChangePanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        locationState = null
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

        render(
            <EmailChangePanel
                currentEmail="old@example.com"
                hasPassword
                linkedProviders={[]}
            />,
        )

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

    it('소셜 회원에게 연결된 계정 재로그인 선택지를 표시한다', () => {
        render(
            <EmailChangePanel
                currentEmail="social@example.com"
                hasPassword={false}
                linkedProviders={['GOOGLE', 'KAKAO']}
            />,
        )

        expect(screen.getByRole('button', { name: 'Google로 본인 확인' })).toBeInTheDocument()
        expect(screen.getByRole('button', { name: '카카오로 본인 확인' })).toBeInTheDocument()
        expect(screen.queryByText('현재 이메일로 인증번호 받기')).not.toBeInTheDocument()
        expect(startEmailChangeOAuthReauthentication).not.toHaveBeenCalled()
    })

    it('소셜 계정 재인증 콜백 후 새 이메일 인증 단계로 이동한다', () => {
        locationState = { emailChangeReauthenticated: true }

        render(
            <EmailChangePanel
                currentEmail="social@example.com"
                hasPassword={false}
                linkedProviders={['NAVER']}
            />,
        )

        expect(screen.getByLabelText('새 이메일')).toBeInTheDocument()
        expect(screen.getByText('소셜 계정 본인 확인이 완료되었습니다.')).toBeInTheDocument()
        expect(navigate).toHaveBeenCalledWith('/mypage', { replace: true, state: null })
    })
})
