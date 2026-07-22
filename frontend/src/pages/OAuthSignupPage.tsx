import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { completeOAuthSignup, confirmOAuthEmailVerification, requestOAuthEmailVerification } from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { AuthField } from '../components/auth/AuthField'
import { AuthMessage } from '../components/auth/AuthMessage'
import { AuthPageLayout } from '../components/auth/AuthPageLayout'

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

    return (
        <AuthPageLayout eyebrow="SOCIAL SIGNUP · KAKAO" title={<>마지막 한 단계만<br />완료해 주세요.</>} description={<>주문과 배송 안내에 사용할 실제 정보를 입력해 주세요.<br className="hidden min-[601px]:block" /> 입력한 정보는 YMall 회원 정보로 안전하게 관리됩니다.</>} asideEyebrow="CONNECTED WITH KAKAO" asideTitle={<>YOUR TASTE,<br />ONE STEP AWAY.</>} asideDescription={<>카카오 계정 연결이 확인되었습니다.<br />추가정보를 입력하면 바로 쇼핑을 시작할 수 있어요.</>} asideClassName="bg-[#fee500] text-center min-[901px]:justify-center">
                {error && <div className="mb-6"><AuthMessage tone="error">{error}</AuthMessage></div>}

                <form className="grid gap-6" onSubmit={submit}>
                    <AuthField id="oauth-email" label="이메일" type="email" value={email} onChange={(event) => { setEmail(event.target.value); setEmailVerified(false); setVerificationSent(false) }} autoComplete="email" placeholder="you@example.com" maxLength={255} required messageId="oauth-email-help" message={<span className="font-normal leading-5 text-[#8b8a82]">이미 가입된 이메일은 기존 계정에서 카카오 연결이 필요합니다.</span>} action={<button className="h-10.5 border border-ink px-4 text-xs font-bold disabled:border-line disabled:text-muted" type="button" onClick={sendVerification} disabled={!email}>인증번호 발송</button>} />
                    {verificationSent && <AuthField id="oauth-verification-code" label="인증번호" inputMode="numeric" value={verificationCode} onChange={(event) => setVerificationCode(event.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="6자리 인증번호" required messageId="oauth-verification-message" message={emailVerified ? <span className="font-normal text-[#657617]" role="status">이메일 인증이 완료되었습니다.</span> : undefined} action={<button className="h-10.5 border border-ink px-4 text-xs font-bold disabled:border-line disabled:text-muted" type="button" onClick={confirmVerification} disabled={verificationCode.length !== 6}>확인</button>} />}
                    <AuthField id="oauth-name" label="이름" value={name} onChange={(event) => setName(event.target.value)} autoComplete="name" placeholder="이름을 입력하세요" maxLength={50} required />
                    <AuthField id="oauth-phone" label="휴대전화 번호" type="tel" value={phone} onChange={(event) => setPhone(event.target.value)} autoComplete="tel" placeholder="01012345678" pattern="01[016789]-?[0-9]{3,4}-?[0-9]{4}" maxLength={13} required />
                    <button className="mt-2 h-13.5 border border-ink bg-ink font-extrabold text-white transition-colors hover:bg-[#333] disabled:cursor-default disabled:opacity-60" disabled={saving || !emailVerified}>
                        {saving ? '가입 정보를 저장하고 있습니다...' : 'YMall 가입 완료'}
                    </button>
                </form>
        </AuthPageLayout>
    )
}
