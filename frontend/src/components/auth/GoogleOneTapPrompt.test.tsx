import { act, render, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { loginWithGoogleOneTap, requestGoogleOneTapNonce } from '../../api/auth'
import { GoogleOneTapPrompt } from './GoogleOneTapPrompt'

vi.mock('../../api/auth', () => ({
    requestGoogleOneTapNonce: vi.fn(),
    loginWithGoogleOneTap: vi.fn(),
}))

describe('GoogleOneTapPrompt', () => {
    const initialize = vi.fn()
    const prompt = vi.fn()
    const cancel = vi.fn()

    beforeEach(() => {
        vi.mocked(requestGoogleOneTapNonce).mockResolvedValue({
            clientId: 'google-web-client-id',
            nonce: 'one-time-nonce',
            expiresIn: 300,
        })
        window.google = {
            accounts: {
                id: {
                    initialize,
                    prompt,
                    cancel,
                    disableAutoSelect: vi.fn(),
                },
            },
        }
    })

    it('서버 nonce로 초기화하고 검증된 기존 회원 로그인을 완료한다', async () => {
        const onAuthenticated = vi.fn()
        const onSignupRequired = vi.fn()
        const onError = vi.fn()
        vi.mocked(loginWithGoogleOneTap).mockResolvedValue({
            signupRequired: false,
            token: {
                accessToken: 'ymall-access-token',
                tokenType: 'Bearer',
                expiresIn: 1800,
            },
        })

        render(
            <GoogleOneTapPrompt
                onAuthenticated={onAuthenticated}
                onSignupRequired={onSignupRequired}
                onError={onError}
            />,
        )

        await waitFor(() => expect(initialize).toHaveBeenCalled())
        const options = initialize.mock.calls[0][0]
        expect(options.client_id).toBe('google-web-client-id')
        expect(options.nonce).toBe('one-time-nonce')

        await act(async () => {
            await options.callback({ credential: 'google-id-token' })
        })

        expect(loginWithGoogleOneTap).toHaveBeenCalledWith('google-id-token')
        expect(onAuthenticated).toHaveBeenCalledWith(
            expect.objectContaining({ accessToken: 'ymall-access-token' }),
        )
        expect(onSignupRequired).not.toHaveBeenCalled()
        expect(onError).not.toHaveBeenCalled()
    })

    it('신규 Google 계정은 추가정보 입력 흐름으로 보낸다', async () => {
        const onAuthenticated = vi.fn()
        const onSignupRequired = vi.fn()
        vi.mocked(loginWithGoogleOneTap).mockResolvedValue({
            signupRequired: true,
            token: null,
        })

        render(
            <GoogleOneTapPrompt
                onAuthenticated={onAuthenticated}
                onSignupRequired={onSignupRequired}
                onError={vi.fn()}
            />,
        )

        await waitFor(() => expect(initialize).toHaveBeenCalled())
        await act(async () => {
            await initialize.mock.calls[0][0].callback({ credential: 'google-id-token' })
        })

        expect(onSignupRequired).toHaveBeenCalledOnce()
        expect(onAuthenticated).not.toHaveBeenCalled()
    })
})
