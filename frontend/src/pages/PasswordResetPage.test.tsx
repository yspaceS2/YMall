import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { confirmPasswordReset, requestPasswordReset, verifyPasswordReset } from '../api/auth'
import { AuthContext, type AuthContextValue } from '../auth/AuthContext'
import { PasswordResetPage } from './PasswordResetPage'

vi.mock('../api/auth', () => ({
    requestPasswordReset: vi.fn(),
    verifyPasswordReset: vi.fn(),
    confirmPasswordReset: vi.fn(),
}))

const auth: AuthContextValue = {
    isAuthenticated: false,
    role: null,
    login: vi.fn(),
    completeOAuthLogin: vi.fn(),
    logout: vi.fn(),
}

function renderPage() {
    return render(
        <AuthContext.Provider value={auth}>
            <MemoryRouter>
                <PasswordResetPage />
            </MemoryRouter>
        </AuthContext.Provider>,
    )
}

describe('PasswordResetPage', () => {
    beforeEach(() => {
        vi.mocked(requestPasswordReset).mockResolvedValue({ requestId: 'request-id-for-password-reset' })
        vi.mocked(verifyPasswordReset).mockResolvedValue({
            resetToken: 'one-time-password-reset-token',
            expiresIn: 600,
        })
        vi.mocked(confirmPasswordReset).mockResolvedValue(undefined)
    })

    it('이메일 인증 후 새 비밀번호를 설정한다', async () => {
        const user = userEvent.setup()
        renderPage()

        await user.type(screen.getByLabelText('이메일'), 'user@example.com')
        await user.click(screen.getByRole('button', { name: '인증번호 받기' }))

        expect(requestPasswordReset).toHaveBeenCalledWith('user@example.com')
        expect(await screen.findByLabelText('인증번호')).toBeInTheDocument()

        await user.type(screen.getByLabelText('인증번호'), '123456')
        await user.click(screen.getByRole('button', { name: '인증번호 확인' }))

        expect(verifyPasswordReset).toHaveBeenCalledWith(
            'request-id-for-password-reset',
            '123456',
        )
        expect(await screen.findByLabelText('새 비밀번호')).toBeInTheDocument()

        await user.type(screen.getByLabelText('새 비밀번호'), 'newPassword123')
        await user.type(screen.getByLabelText('새 비밀번호 확인'), 'differentPassword')
        expect(screen.getByText('비밀번호가 일치하지 않습니다.')).toBeInTheDocument()
        expect(screen.getByRole('button', { name: '비밀번호 재설정' })).toBeDisabled()

        await user.clear(screen.getByLabelText('새 비밀번호 확인'))
        await user.type(screen.getByLabelText('새 비밀번호 확인'), 'newPassword123')
        await user.click(screen.getByRole('button', { name: '비밀번호 재설정' }))

        expect(confirmPasswordReset).toHaveBeenCalledWith({
            resetToken: 'one-time-password-reset-token',
            newPassword: 'newPassword123',
            newPasswordConfirmation: 'newPassword123',
        })
        expect(await screen.findByText('비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해 주세요.'))
            .toBeInTheDocument()
    })

    it('인증 실패 메시지를 표시하고 인증 단계에 머문다', async () => {
        const user = userEvent.setup()
        vi.mocked(verifyPasswordReset).mockRejectedValue(new Error('verification failed'))
        renderPage()

        await user.type(screen.getByLabelText('이메일'), 'user@example.com')
        await user.click(screen.getByRole('button', { name: '인증번호 받기' }))
        await user.type(await screen.findByLabelText('인증번호'), '000000')
        await user.click(screen.getByRole('button', { name: '인증번호 확인' }))

        expect(await screen.findByRole('alert')).toHaveTextContent(
            '요청 처리 중 오류가 발생했습니다. 다시 시도해 주세요.',
        )
        expect(screen.getByLabelText('인증번호')).toBeInTheDocument()
    })
})
