import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'

import { ApiError } from '../api/client'
import { ToastProvider } from '../toast/ToastProvider'
import { LoginPage } from './LoginPage'

const authMocks = vi.hoisted(() => ({
    login: vi.fn(),
    completeOAuthLogin: vi.fn(),
}))

vi.mock('../auth/useAuth', () => ({
    useAuth: () => ({
        isAuthenticated: false,
        login: authMocks.login,
        completeOAuthLogin: authMocks.completeOAuthLogin,
    }),
}))

vi.mock('../components/auth/GoogleOneTapPrompt', () => ({
    GoogleOneTapPrompt: ({ onError }: { onError: (error: unknown) => void }) => (
        <button type="button" onClick={() => onError(new Error('Google One Tap unavailable'))}>
            Google One Tap 오류 발생
        </button>
    ),
}))

vi.mock('../api/googleIdentity', () => ({
    cancelGoogleOneTap: vi.fn(),
}))

function renderLoginPage() {
    return render(
        <MemoryRouter>
            <ToastProvider>
                <LoginPage />
            </ToastProvider>
        </MemoryRouter>,
    )
}

describe('LoginPage', () => {
    it('로그인 실패를 입력 폼 박스 대신 토스트로 표시한다', async () => {
        authMocks.login.mockRejectedValueOnce(
            new ApiError('이메일 또는 비밀번호를 확인해 주세요.', 401, 'LOGIN_FAILED'),
        )
        const user = userEvent.setup()
        renderLoginPage()

        await user.type(screen.getByRole('textbox', { name: '이메일' }), 'user@ymall.local')
        await user.type(screen.getByLabelText('비밀번호'), 'WrongPassword!')
        await user.click(screen.getByRole('button', { name: '로그인' }))

        expect(await screen.findByRole('alert')).toHaveTextContent(
            '이메일 또는 비밀번호를 확인해 주세요.',
        )
        expect(screen.queryByText('이메일 또는 비밀번호를 확인해 주세요.', {
            selector: 'form *',
        })).not.toBeInTheDocument()
    })

    it('Google One Tap 초기화 오류를 토스트로 표시한다', async () => {
        const user = userEvent.setup()
        renderLoginPage()

        await user.click(screen.getByRole('button', { name: 'Google One Tap 오류 발생' }))

        expect(await screen.findByRole('alert')).toHaveTextContent(
            'Google 간편 로그인을 사용할 수 없습니다. 다른 로그인 방법을 이용해 주세요.',
        )
    })
})
