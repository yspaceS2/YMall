import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { refreshAccessToken } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { AuthMessage } from '../components/auth/AuthMessage'
import { AuthPageLayout } from '../components/auth/AuthPageLayout'

export function OAuth2CallbackPage() {
    const navigate = useNavigate()
    const { completeOAuthLogin } = useAuth()
    const parameters = new URLSearchParams(window.location.hash.slice(1))
    const loginCompleted = parameters.get('loginCompleted') === 'true'
    const signupRequired = parameters.get('signupRequired') === 'true'
    const errorMessage = parameters.get('error')
    const emailChangeReauthenticated =
        parameters.get('emailChangeReauthenticated') === 'true'
    const emailChangeReauthenticationError =
        parameters.get('emailChangeReauthenticationError')
    const [loginCompletionError, setLoginCompletionError] = useState<string | null>(null)

    useEffect(() => {
        if (emailChangeReauthenticated) {
            window.history.replaceState(null, '', window.location.pathname)
            navigate('/mypage', {
                replace: true,
                state: { emailChangeReauthenticated: true },
            })
        } else if (emailChangeReauthenticationError) {
            window.history.replaceState(null, '', window.location.pathname)
            navigate('/mypage', {
                replace: true,
                state: { emailChangeReauthenticationError },
            })
        } else if (signupRequired) {
            window.history.replaceState(null, '', window.location.pathname)
            navigate('/oauth2/signup', { replace: true })
        } else if (loginCompleted) {
            let active = true
            refreshAccessToken()
                .then((accessToken) => {
                    if (!active) return
                    window.history.replaceState(null, '', window.location.pathname)
                    if (!accessToken) {
                        setLoginCompletionError('소셜 로그인 토큰을 발급하지 못했습니다.')
                        return
                    }
                    completeOAuthLogin(accessToken)
                    navigate('/', { replace: true })
                })
                .catch(() => {
                    if (!active) return
                    window.history.replaceState(null, '', window.location.pathname)
                    setLoginCompletionError('소셜 로그인 토큰을 발급하지 못했습니다.')
                })
            return () => {
                active = false
            }
        }
    }, [
        completeOAuthLogin,
        emailChangeReauthenticated,
        emailChangeReauthenticationError,
        loginCompleted,
        navigate,
        signupRequired,
    ])

    const hasError = Boolean(
        loginCompletionError
        || (!loginCompleted
            && !signupRequired
            && !emailChangeReauthenticated
            && !emailChangeReauthenticationError),
    )

    return (
        <AuthPageLayout eyebrow="SOCIAL LOGIN" title={hasError ? '로그인에 실패했습니다.' : '본인 확인을 완료하고 있습니다.'} description={hasError ? '소셜 로그인 과정에서 문제가 발생했습니다.' : '잠시만 기다려 주세요.'} asideEyebrow="YMALL MEMBERS" asideTitle={<>YOUR TASTE,<br />CONNECTED.</>} contentClassName="max-w-xl self-center">
            {hasError ? (
                <div>
                    <AuthMessage tone="error">{loginCompletionError ?? errorMessage ?? '소셜 로그인 결과를 확인할 수 없습니다.'}</AuthMessage>
                    <button className="mt-8 h-12 border border-ink bg-ink px-8 font-bold text-white" onClick={() => navigate('/login', { replace: true })}>
                        로그인으로 돌아가기
                    </button>
                </div>
            ) : (
                <AuthMessage>소셜 로그인을 완료하고 있습니다...</AuthMessage>
            )}
        </AuthPageLayout>
    )
}
