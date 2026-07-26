import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
    checkEmailAvailability,
    confirmSignupEmailVerification,
    requestSignupEmailVerification,
    signupMember,
} from '../api/auth'
import { SignupPage } from './SignupPage'

const navigate = vi.fn()

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
    return {
        ...actual,
        useNavigate: () => navigate,
    }
})

vi.mock('../auth/useAuth', () => ({
    useAuth: () => ({ isAuthenticated: false }),
}))

vi.mock('../api/auth', () => ({
    checkEmailAvailability: vi.fn(),
    requestSignupEmailVerification: vi.fn(),
    confirmSignupEmailVerification: vi.fn(),
    signupMember: vi.fn(),
}))

describe('SignupPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(checkEmailAvailability).mockResolvedValue({ available: true })
        vi.mocked(requestSignupEmailVerification).mockResolvedValue({
            requestId: 'signup-request',
            expiresIn: 300,
        })
        vi.mocked(confirmSignupEmailVerification).mockResolvedValue({
            verificationToken: 'verified-token',
            expiresIn: 600,
        })
        vi.mocked(signupMember).mockResolvedValue({
            memberId: 1,
            email: 'user@example.com',
            name: '홍길동',
            phone: '01012345678',
            role: 'ROLE_USER',
            createdAt: '2026-07-26T00:00:00',
        })
    })

    it('이메일 인증을 완료한 뒤 회원가입 요청에 일회성 토큰을 포함한다', async () => {
        render(
            <MemoryRouter>
                <SignupPage />
            </MemoryRouter>,
        )

        fireEvent.change(screen.getByLabelText('이메일'), {
            target: { value: 'User@Example.com' },
        })
        fireEvent.click(screen.getByRole('button', { name: '중복 확인' }))
        await screen.findByText('사용 가능한 이메일입니다. 인증번호를 발송해 주세요.')

        fireEvent.click(screen.getByRole('button', { name: '인증번호 발송' }))
        await screen.findByLabelText('이메일 인증번호')

        fireEvent.change(screen.getByLabelText('이메일 인증번호'), {
            target: { value: '123456' },
        })
        fireEvent.click(screen.getByRole('button', { name: '인증 확인' }))
        await screen.findByText('이메일 인증이 완료되었습니다.')

        fireEvent.change(screen.getByLabelText('이름'), {
            target: { value: '홍길동' },
        })
        fireEvent.change(screen.getByLabelText('휴대전화 번호'), {
            target: { value: '010-1234-5678' },
        })
        fireEvent.change(screen.getByLabelText('비밀번호'), {
            target: { value: 'password123' },
        })
        fireEvent.change(screen.getByLabelText('비밀번호 확인'), {
            target: { value: 'password123' },
        })
        fireEvent.click(screen.getByRole('button', { name: '회원가입' }))

        await waitFor(() => {
            expect(signupMember).toHaveBeenCalledWith({
                email: 'User@Example.com',
                emailVerificationToken: 'verified-token',
                password: 'password123',
                passwordConfirmation: 'password123',
                name: '홍길동',
                phone: '01012345678',
            })
            expect(navigate).toHaveBeenCalledWith('/login', {
                replace: true,
                state: { signupCompleted: true },
            })
        })
    })

    it('인증 후 이메일을 바꾸면 인증 상태를 초기화한다', async () => {
        render(
            <MemoryRouter>
                <SignupPage />
            </MemoryRouter>,
        )

        fireEvent.change(screen.getByLabelText('이메일'), {
            target: { value: 'user@example.com' },
        })
        fireEvent.click(screen.getByRole('button', { name: '중복 확인' }))
        await screen.findByText('사용 가능한 이메일입니다. 인증번호를 발송해 주세요.')
        fireEvent.click(screen.getByRole('button', { name: '인증번호 발송' }))
        fireEvent.change(await screen.findByLabelText('이메일 인증번호'), {
            target: { value: '123456' },
        })
        fireEvent.click(screen.getByRole('button', { name: '인증 확인' }))
        await screen.findByText('이메일 인증이 완료되었습니다.')

        fireEvent.change(screen.getByLabelText('이메일'), {
            target: { value: 'other@example.com' },
        })

        expect(screen.getByRole('button', { name: '중복 확인' })).toBeInTheDocument()
        expect(screen.queryByText('이메일 인증이 완료되었습니다.')).not.toBeInTheDocument()
    })
})
