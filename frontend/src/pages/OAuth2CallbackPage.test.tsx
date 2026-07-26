import { render, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { OAuth2CallbackPage } from './OAuth2CallbackPage'

const navigate = vi.fn()
const completeOAuthLogin = vi.fn()

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigate,
}))

vi.mock('../auth/useAuth', () => ({
    useAuth: () => ({ completeOAuthLogin }),
}))

describe('OAuth2CallbackPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    afterEach(() => {
        window.history.replaceState(null, '', '/')
    })

    it('이메일 변경 소셜 재인증 성공 시 마이페이지로 돌아간다', async () => {
        window.location.hash = '#emailChangeReauthenticated=true'

        render(<OAuth2CallbackPage />)

        await waitFor(() => {
            expect(navigate).toHaveBeenCalledWith('/mypage', {
                replace: true,
                state: { emailChangeReauthenticated: true },
            })
        })
        expect(completeOAuthLogin).not.toHaveBeenCalled()
    })

    it('이메일 변경 소셜 재인증 실패 메시지를 마이페이지로 전달한다', async () => {
        window.location.hash =
            '#emailChangeReauthenticationError=linked+account+required'

        render(<OAuth2CallbackPage />)

        await waitFor(() => {
            expect(navigate).toHaveBeenCalledWith('/mypage', {
                replace: true,
                state: {
                    emailChangeReauthenticationError: 'linked account required',
                },
            })
        })
        expect(completeOAuthLogin).not.toHaveBeenCalled()
    })
})
