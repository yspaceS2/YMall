import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { AuthMessage } from '../components/auth/AuthMessage'
import { AuthPageLayout } from '../components/auth/AuthPageLayout'

export function OAuth2CallbackPage() {
    const navigate = useNavigate()
    const { completeOAuthLogin } = useAuth()
    const parameters = new URLSearchParams(window.location.hash.slice(1))
    const accessToken = parameters.get('accessToken')
    const signupRequired = parameters.get('signupRequired') === 'true'
    const errorMessage = parameters.get('error')
    const emailChangeReauthenticated =
        parameters.get('emailChangeReauthenticated') === 'true'
    const emailChangeReauthenticationError =
        parameters.get('emailChangeReauthenticationError')

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
        } else if (accessToken) {
            completeOAuthLogin(accessToken)
            window.history.replaceState(null, '', window.location.pathname)
            navigate('/', { replace: true })
        }
    }, [
        accessToken,
        completeOAuthLogin,
        emailChangeReauthenticated,
        emailChangeReauthenticationError,
        navigate,
        signupRequired,
    ])

    return (
        <AuthPageLayout eyebrow="SOCIAL LOGIN" title={!accessToken && !signupRequired && !emailChangeReauthenticated && !emailChangeReauthenticationError ? '로그인에 실패했습니다.' : '본인 확인을 완료하고 있습니다.'} description={!accessToken && !signupRequired && !emailChangeReauthenticated && !emailChangeReauthenticationError ? '소셜 로그인 과정에서 문제가 발생했습니다.' : '잠시만 기다려 주세요.'} asideEyebrow="YMALL MEMBERS" asideTitle={<>YOUR TASTE,<br />CONNECTED.</>} contentClassName="max-w-xl self-center">
            {!accessToken && !signupRequired && !emailChangeReauthenticated && !emailChangeReauthenticationError ? (
                <div>
                    <AuthMessage tone="error">{errorMessage ?? '소셜 로그인 결과를 확인할 수 없습니다.'}</AuthMessage>
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
