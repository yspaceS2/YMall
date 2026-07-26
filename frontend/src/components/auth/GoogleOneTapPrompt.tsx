import { useEffect, useRef } from 'react'
import { loginWithGoogleOneTap, requestGoogleOneTapNonce } from '../../api/auth'
import { cancelGoogleOneTap, loadGoogleIdentityScript } from '../../api/googleIdentity'
import type { TokenResponse } from '../../types/auth'

interface GoogleOneTapPromptProps {
    onAuthenticated: (token: TokenResponse) => void
    onSignupRequired: () => void
    onError: (error: unknown) => void
}

export function GoogleOneTapPrompt({
    onAuthenticated,
    onSignupRequired,
    onError,
}: GoogleOneTapPromptProps) {
    const callbacks = useRef({ onAuthenticated, onSignupRequired, onError })

    useEffect(() => {
        callbacks.current = { onAuthenticated, onSignupRequired, onError }
    }, [onAuthenticated, onError, onSignupRequired])

    useEffect(() => {
        let active = true

        async function initialize() {
            try {
                const [nonceResponse] = await Promise.all([
                    requestGoogleOneTapNonce(),
                    loadGoogleIdentityScript(),
                ])
                if (!active || !window.google?.accounts.id) {
                    return
                }

                window.google.accounts.id.initialize({
                    client_id: nonceResponse.clientId,
                    nonce: nonceResponse.nonce,
                    auto_select: false,
                    cancel_on_tap_outside: true,
                    itp_support: true,
                    use_fedcm_for_prompt: true,
                    callback: async (response) => {
                        if (!active || !response.credential) {
                            return
                        }
                        try {
                            const result = await loginWithGoogleOneTap(response.credential)
                            if (!active) {
                                return
                            }
                            if (result.signupRequired) {
                                callbacks.current.onSignupRequired()
                            } else if (result.token) {
                                callbacks.current.onAuthenticated(result.token)
                            } else {
                                callbacks.current.onError(
                                    new Error('Google 로그인 응답이 올바르지 않습니다.'),
                                )
                            }
                        } catch (error) {
                            if (active) {
                                callbacks.current.onError(error)
                            }
                        }
                    },
                })
                window.google.accounts.id.prompt()
            } catch (error) {
                if (active) {
                    callbacks.current.onError(error)
                }
            }
        }

        void initialize()
        return () => {
            active = false
            cancelGoogleOneTap()
        }
    }, [])

    return null
}
