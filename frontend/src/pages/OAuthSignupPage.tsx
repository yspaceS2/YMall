import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { completeOAuthSignup, confirmOAuthEmailVerification, requestOAuthEmailVerification } from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'

export function OAuthSignupPage() {
    const navigate = useNavigate()
    const { completeOAuthLogin } = useAuth()
    const [email, setEmail] = useState('')
    const [name, setName] = useState('')
    const [phone, setPhone] = useState('')
    const [error, setError] = useState('')
    const [saving, setSaving] = useState(false)
    const [verificationCode, setVerificationCode] = useState('')
    const [verificationSent, setVerificationSent] = useState(false)
    const [emailVerified, setEmailVerified] = useState(false)

    async function sendVerification() {
        setError('')
        try {
            await requestOAuthEmailVerification(email.trim())
            setVerificationSent(true)
            setEmailVerified(false)
        } catch (cause) {
            setError(cause instanceof ApiError ? cause.message : '인증 이메일을 발송하지 못했습니다.')
        }
    }

    async function confirmVerification() {
        setError('')
        try {
            await confirmOAuthEmailVerification(email.trim(), verificationCode)
            setEmailVerified(true)
        } catch (cause) {
            setError(cause instanceof ApiError ? cause.message : '이메일 인증에 실패했습니다.')
        }
    }

    async function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError('')
        setSaving(true)
        try {
            const token = await completeOAuthSignup({ email: email.trim(), name: name.trim(), phone: phone.replace(/[\s-]/g, '') })
            completeOAuthLogin(token.accessToken)
            navigate('/', { replace: true })
        } catch (cause) {
            setError(cause instanceof ApiError ? cause.message : '소셜 회원가입을 완료하지 못했습니다.')
        } finally {
            setSaving(false)
        }
    }

    const inputClassName = 'w-full border-0 border-b border-line bg-transparent px-0.5 py-3.5 text-sm text-ink outline-0 transition-colors placeholder:text-[#aaa99f] focus:border-ink'

    return (
        <section className="grid min-h-[calc(100vh-76px)] grid-cols-1 min-[901px]:grid-cols-[minmax(0,1.08fr)_minmax(390px,.92fr)]">
            <div className="mx-auto w-[calc(100%-40px)] max-w-125 py-14 min-[601px]:w-[calc(100%-48px)] min-[601px]:py-20">
                <div className="mb-9 flex items-center justify-between border-b border-line pb-4">
                    <p className="text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">SOCIAL SIGNUP</p>
                    <span className="rounded-full bg-[#fee500] px-3 py-1.5 text-[10px] font-extrabold tracking-[.08em] text-[#191919]">KAKAO</span>
                </div>
                <h1 className="font-serif text-[clamp(40px,5vw,62px)] leading-[1.02] font-medium tracking-[-.05em]">마지막 한 단계만<br />완료해 주세요.</h1>
                <p className="mt-5 text-sm leading-7 text-muted" style={{ marginBottom: '64px' }}>주문과 배송 안내에 사용할 실제 정보를 입력해 주세요.<br className="hidden min-[601px]:block" /> 입력한 정보는 YMall 회원 정보로 안전하게 관리됩니다.</p>

                {error && <p className="mt-7 border border-[#d9aaa4] bg-[#f9ecea] px-4 py-3 text-sm text-[#b23b2f]" role="alert">{error}</p>}

                <form className="grid gap-6" onSubmit={submit}>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>이메일</span>
                        <div className="flex gap-2">
                            <input className={inputClassName} type="email" value={email} onChange={(event) => { setEmail(event.target.value); setEmailVerified(false); setVerificationSent(false) }} autoComplete="email" placeholder="you@example.com" maxLength={255} required />
                            <button className="shrink-0 border border-ink px-4 text-xs font-bold" type="button" onClick={sendVerification} disabled={!email}>인증번호 발송</button>
                        </div>
                        <span className="font-normal leading-5 text-[#8b8a82]">이미 가입된 이메일은 기존 계정에서 카카오 연결이 필요합니다.</span>
                    </label>
                    {verificationSent && <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>인증번호</span>
                        <div className="flex gap-2">
                            <input className={inputClassName} inputMode="numeric" value={verificationCode} onChange={(event) => setVerificationCode(event.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="6자리 인증번호" required />
                            <button className="shrink-0 border border-ink px-4 text-xs font-bold" type="button" onClick={confirmVerification} disabled={verificationCode.length !== 6}>확인</button>
                        </div>
                        {emailVerified && <span className="font-normal text-[#657617]">이메일 인증이 완료되었습니다.</span>}
                    </label>}
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>이름</span>
                        <input className={inputClassName} value={name} onChange={(event) => setName(event.target.value)} autoComplete="name" placeholder="이름을 입력하세요" maxLength={50} required />
                    </label>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>휴대전화 번호</span>
                        <input className={inputClassName} type="tel" value={phone} onChange={(event) => setPhone(event.target.value)} autoComplete="tel" placeholder="01012345678" pattern="01[016789]-?[0-9]{3,4}-?[0-9]{4}" maxLength={13} required />
                    </label>
                    <button className="mt-2 h-13.5 border border-ink bg-ink font-extrabold text-white transition-colors hover:bg-[#333] disabled:cursor-default disabled:opacity-60" disabled={saving || !emailVerified}>
                        {saving ? '가입 정보를 저장하고 있습니다...' : 'YMall 가입 완료'}
                    </button>
                </form>
            </div>

            <aside className="flex min-h-72 items-center justify-center overflow-hidden bg-[#fee500] px-8 py-14 text-center text-[#191919] min-[601px]:min-h-90 min-[901px]:px-12" aria-hidden="true">
                <div className="mx-auto w-full max-w-105">
                    <div className="mx-auto mb-9 h-px w-14 bg-[#191919]" />
                    <p className="mb-6 text-[11px] font-extrabold tracking-[.2em]">CONNECTED WITH KAKAO</p>
                    <strong className="block font-serif text-[clamp(42px,5vw,72px)] leading-[.94] font-medium tracking-[-.06em]">YOUR TASTE,<br />ONE STEP AWAY.</strong>
                    <p className="mx-auto mt-8 max-w-80 text-sm leading-7 text-[#4a4300]">카카오 계정 연결이 확인되었습니다.<br />추가정보를 입력하면 바로 쇼핑을 시작할 수 있어요.</p>
                </div>
            </aside>
        </section>
    )
}
