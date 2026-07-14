import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'

interface LoginLocationState {
    from?: string
}

export function LoginPage() {
    const { isAuthenticated, login } = useAuth()
    const location = useLocation()
    const navigate = useNavigate()
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [errorMessage, setErrorMessage] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)
    const state = location.state as LoginLocationState | null
    const destination = state?.from?.startsWith('/') ? state.from : '/'

    if (isAuthenticated) {
        return <Navigate to={destination} replace />
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setErrorMessage('')
        setIsSubmitting(true)

        try {
            await login({ email, password })
            navigate(destination, { replace: true })
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError ? error.message : '로그인 중 오류가 발생했습니다.',
            )
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <section className="login-page">
            <div className="login-panel">
                <p className="login-eyebrow">MEMBER ACCESS</p>
                <h1>다시 만나 반갑습니다.</h1>
                <p className="login-description">로그인하고 장바구니와 주문 내역을 이어서 확인하세요.</p>

                <form className="login-form" onSubmit={handleSubmit}>
                    <label>
                        <span>이메일</span>
                        <input
                            type="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            autoComplete="email"
                            placeholder="you@example.com"
                            required
                        />
                    </label>
                    <label>
                        <span>비밀번호</span>
                        <input
                            type="password"
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                            autoComplete="current-password"
                            placeholder="비밀번호를 입력하세요"
                            required
                        />
                    </label>
                    {errorMessage && <p className="login-error" role="alert">{errorMessage}</p>}
                    <button type="submit" disabled={isSubmitting}>
                        {isSubmitting ? '로그인 중...' : '로그인'}
                    </button>
                </form>
            </div>
            <aside className="login-visual" aria-hidden="true">
                <span>YMALL MEMBERS</span>
                <strong>YOUR TASTE,<br />STILL HERE.</strong>
            </aside>
        </section>
    )
}
