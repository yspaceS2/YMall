import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

export function OAuth2CallbackPage() {
    const navigate = useNavigate()
    const { completeOAuthLogin } = useAuth()
    const parameters = new URLSearchParams(window.location.hash.slice(1))
    const accessToken = parameters.get('accessToken')
    const signupRequired = parameters.get('signupRequired') === 'true'
    const errorMessage = parameters.get('error')

    useEffect(() => {
        if (signupRequired) {
            window.history.replaceState(null, '', window.location.pathname)
            navigate('/oauth2/signup', { replace: true })
        } else if (accessToken) {
            completeOAuthLogin(accessToken)
            window.history.replaceState(null, '', window.location.pathname)
            navigate('/', { replace: true })
        }
    }, [accessToken, completeOAuthLogin, navigate, signupRequired])

    return (
        <section className="mx-auto grid min-h-[calc(100vh-76px)] max-w-xl place-content-center px-5 text-center">
            {!accessToken && !signupRequired ? (
                <>
                    <h1 className="font-serif text-4xl">로그인에 실패했습니다</h1>
                    <p className="mt-4 text-sm text-[#b23b2f]" role="alert">{errorMessage ?? '소셜 로그인 결과를 확인할 수 없습니다.'}</p>
                    <button className="mt-8 h-12 border border-ink bg-ink px-8 font-bold text-white" onClick={() => navigate('/login', { replace: true })}>
                        로그인으로 돌아가기
                    </button>
                </>
            ) : (
                <p className="text-sm text-muted" role="status">소셜 로그인을 완료하고 있습니다...</p>
            )}
        </section>
    )
}
