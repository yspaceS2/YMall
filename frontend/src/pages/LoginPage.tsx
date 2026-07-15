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
        <section className="grid min-h-[calc(100vh-76px)] grid-cols-1 lg:grid-cols-[minmax(0,.9fr)_minmax(420px,1.1fr)]">
            <div className="mx-auto w-[calc(100%-40px)] max-w-115 py-14 sm:w-[calc(100%-48px)] sm:py-20">
                <p className="mb-4.5 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">MEMBER ACCESS</p>
                <h1 className="m-0 font-serif text-[clamp(38px,5vw,62px)] leading-none font-medium tracking-[-.05em]">다시 만나 반갑습니다.</h1>
                <p className="mt-5 mb-10.5 text-sm leading-7 text-muted">로그인하고 장바구니와 주문 내역을 이어서 확인하세요.</p>

                <form className="grid gap-6" onSubmit={handleSubmit}>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>이메일</span>
                        <input
                            className="w-full border-0 border-b border-line bg-transparent px-0.5 py-3.5 text-ink outline-0 focus:border-ink"
                            type="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            autoComplete="email"
                            placeholder="you@example.com"
                            required
                        />
                    </label>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>비밀번호</span>
                        <input
                            className="w-full border-0 border-b border-line bg-transparent px-0.5 py-3.5 text-ink outline-0 focus:border-ink"
                            type="password"
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                            autoComplete="current-password"
                            placeholder="비밀번호를 입력하세요"
                            required
                        />
                    </label>
                    {errorMessage && <p className="-mt-2 text-xs text-[#b23b2f]" role="alert">{errorMessage}</p>}
                    <button className="mt-2 h-13.5 border border-ink bg-ink font-extrabold text-white disabled:cursor-wait disabled:opacity-60" type="submit" disabled={isSubmitting}>
                        {isSubmitting ? '로그인 중...' : '로그인'}
                    </button>
                </form>
            </div>
            <aside className="flex min-h-75 flex-col justify-end bg-[radial-gradient(circle_at_75%_22%,rgba(217,255,67,.95),transparent_21%),linear-gradient(145deg,#d9ddc8,#f1f0e8_58%,#c8cfab)] p-5 text-ink sm:min-h-95 sm:p-[clamp(40px,7vw,100px)]" aria-hidden="true">
                <span className="mb-4.5 text-[11px] font-extrabold tracking-[.2em]">YMALL MEMBERS</span>
                <strong className="font-serif text-[clamp(48px,6vw,90px)] leading-[.88] font-medium tracking-[-.06em]">YOUR TASTE,<br />STILL HERE.</strong>
            </aside>
        </section>
    )
}
