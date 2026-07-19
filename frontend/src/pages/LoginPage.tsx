import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { clearAccessToken } from '../auth/tokenStorage'

interface LoginLocationState {
    from?: string
    signupCompleted?: boolean
}

function GoogleLogo() {
    return (
        <svg aria-hidden="true" viewBox="0 0 24 24" className="h-5 w-5">
            <path fill="#4285F4" d="M21.6 12.23c0-.71-.06-1.4-.18-2.07H12v3.92h5.38a4.6 4.6 0 0 1-2 3.02v2.54h3.24c1.9-1.75 2.98-4.33 2.98-7.41Z" />
            <path fill="#34A853" d="M12 22c2.7 0 4.97-.9 6.62-2.36l-3.24-2.54c-.9.6-2.05.96-3.38.96-2.61 0-4.82-1.76-5.61-4.13H3.05v2.62A10 10 0 0 0 12 22Z" />
            <path fill="#FBBC05" d="M6.39 13.93A6.02 6.02 0 0 1 6.07 12c0-.67.12-1.32.32-1.93V7.45H3.05A10 10 0 0 0 2 12c0 1.61.39 3.14 1.05 4.55l3.34-2.62Z" />
            <path fill="#EA4335" d="M12 5.94c1.47 0 2.79.5 3.83 1.5l2.87-2.87A9.62 9.62 0 0 0 12 2a10 10 0 0 0-8.95 5.45l3.34 2.62C7.18 7.7 9.39 5.94 12 5.94Z" />
        </svg>
    )
}

function KakaoLogo() {
    return (
        <svg aria-hidden="true" viewBox="0 0 24 24" className="h-5 w-5 fill-[#191919]">
            <path d="M12 3C6.48 3 2 6.54 2 10.91c0 2.82 1.87 5.29 4.68 6.69l-.95 3.49a.38.38 0 0 0 .58.41l4.14-2.76c.5.05 1.02.08 1.55.08 5.52 0 10-3.54 10-7.91S17.52 3 12 3Z" />
        </svg>
    )
}

function NaverLogo() {
    return (
        <svg aria-hidden="true" viewBox="0 0 24 24" className="h-5 w-5">
            <path fill="#fff" d="M5 4h5.1l3.8 5.5V4H19v16h-5.1l-3.8-5.5V20H5V4Z" />
        </svg>
    )
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
        <section className="grid min-h-[calc(100vh-76px)] grid-cols-1 min-[901px]:grid-cols-[minmax(0,.9fr)_minmax(420px,1.1fr)]">
            <div className="mx-auto w-[calc(100%-40px)] max-w-115 py-14 min-[601px]:w-[calc(100%-48px)] min-[601px]:py-20">
                <p className="mb-4.5 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">MEMBER ACCESS</p>
                <h1 className="m-0 font-serif text-[clamp(38px,5vw,62px)] leading-none font-medium tracking-[-.05em]">다시 만나 반갑습니다.</h1>
                <p className="mt-5 mb-10.5 text-sm leading-7 text-muted">로그인하고 장바구니와 주문 내역을 이어서 확인하세요.</p>

                {state?.signupCompleted && (
                    <p className="mb-6 border border-[#a7b866] bg-[#eef3d8] px-4 py-3 text-sm text-[#55620f]" role="status">
                        회원가입이 완료되었습니다. 새 계정으로 로그인해 주세요.
                    </p>
                )}

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
                <div className="my-7 flex items-center gap-4 text-[11px] font-bold tracking-[.14em] text-muted">
                    <span className="h-px flex-1 bg-line" />
                    OR
                    <span className="h-px flex-1 bg-line" />
                </div>
                <div className="grid gap-3">
                    <a className="relative grid h-12 place-items-center border border-[#dadce0] bg-white px-12 text-sm font-bold text-[#3c4043]" href="/oauth2/authorization/google" onClick={clearAccessToken}>
                        <span className="absolute left-4"><GoogleLogo /></span>
                        <span>Google로 계속하기</span>
                    </a>
                    <a className="relative grid h-12 place-items-center bg-[#fee500] px-12 text-sm font-bold text-[#191919]" href="/oauth2/authorization/kakao" onClick={clearAccessToken}>
                        <span className="absolute left-4"><KakaoLogo /></span>
                        <span>카카오로 계속하기</span>
                    </a>
                    <a className="relative grid h-12 place-items-center bg-[#03c75a] px-12 text-sm font-bold text-white" href="/oauth2/authorization/naver" onClick={clearAccessToken}>
                        <span className="absolute left-4"><NaverLogo /></span>
                        <span>네이버로 계속하기</span>
                    </a>
                </div>
                <p className="mt-7 text-sm text-muted">
                    아직 회원이 아니신가요?{' '}
                    <Link className="font-bold text-ink underline underline-offset-4" to="/signup">
                        회원가입
                    </Link>
                </p>
            </div>
            <aside className="flex min-h-75 flex-col justify-end bg-[radial-gradient(circle_at_75%_22%,rgba(217,255,67,.95),transparent_21%),linear-gradient(145deg,#d9ddc8,#f1f0e8_58%,#c8cfab)] p-5 text-ink min-[601px]:min-h-95 min-[601px]:p-[clamp(40px,7vw,100px)]" aria-hidden="true">
                <span className="mb-4.5 text-[11px] font-extrabold tracking-[.2em]">YMALL MEMBERS</span>
                <strong className="font-serif text-[clamp(48px,6vw,90px)] leading-[.88] font-medium tracking-[-.06em]">YOUR TASTE,<br />STILL HERE.</strong>
            </aside>
        </section>
    )
}
