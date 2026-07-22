import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { checkEmailAvailability, signupMember } from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { AuthField } from '../components/auth/AuthField'
import { AuthMessage } from '../components/auth/AuthMessage'
import { AuthPageLayout } from '../components/auth/AuthPageLayout'

interface SignupForm {
    email: string
    password: string
    passwordConfirmation: string
    name: string
    phone: string
}

const initialForm: SignupForm = {
    email: '',
    password: '',
    passwordConfirmation: '',
    name: '',
    phone: '',
}

type EmailCheckStatus = 'idle' | 'checking' | 'available' | 'unavailable' | 'error'

interface EmailCheckState {
    email: string
    status: EmailCheckStatus
}

export function SignupPage() {
    const { isAuthenticated } = useAuth()
    const navigate = useNavigate()
    const [form, setForm] = useState(initialForm)
    const [errorMessage, setErrorMessage] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [emailCheck, setEmailCheck] = useState<EmailCheckState>({ email: '', status: 'idle' })

    if (isAuthenticated) {
        return <Navigate to="/" replace />
    }

    const updateField = (field: keyof SignupForm, value: string) => {
        setForm((current) => ({ ...current, [field]: value }))
        if (field === 'email') {
            setEmailCheck({ email: value.trim().toLowerCase(), status: 'idle' })
        }
    }

    const normalizedEmail = form.email.trim().toLowerCase()
    const isEmailFormatValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)
    const currentEmailCheckStatus = emailCheck.email === normalizedEmail ? emailCheck.status : 'idle'
    const isEmailAvailable = currentEmailCheckStatus === 'available'
    const hasEmailCheckMessage = currentEmailCheckStatus === 'available'
        || currentEmailCheckStatus === 'unavailable'
        || currentEmailCheckStatus === 'error'
    const hasPasswordConfirmation = form.passwordConfirmation.length > 0
    const isPasswordMatched = hasPasswordConfirmation && form.password === form.passwordConfirmation
    const canSubmit = isEmailAvailable && isPasswordMatched && !isSubmitting

    async function handleEmailAvailabilityCheck() {
        if (!isEmailFormatValid) return

        const email = normalizedEmail
        setEmailCheck({ email, status: 'checking' })
        try {
            const response = await checkEmailAvailability(email)
            setEmailCheck({
                email,
                status: response.available ? 'available' : 'unavailable',
            })
        } catch {
            setEmailCheck({ email, status: 'error' })
        }
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setErrorMessage('')

        if (!isEmailAvailable) {
            setErrorMessage('이메일 중복 확인을 완료해 주세요.')
            return
        }

        if (form.password !== form.passwordConfirmation) {
            setErrorMessage('비밀번호 확인이 일치하지 않습니다.')
            return
        }

        setIsSubmitting(true)
        try {
            await signupMember({
                ...form,
                email: form.email.trim(),
                name: form.name.trim(),
                phone: form.phone.replace(/[\s-]/g, ''),
            })
            navigate('/login', {
                replace: true,
                state: { signupCompleted: true },
            })
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError ? error.message : '회원가입 중 오류가 발생했습니다.',
            )
        } finally {
            setIsSubmitting(false)
        }
    }

    const fields: Array<{
        key: keyof SignupForm
        label: string
        type: string
        autoComplete: string
        placeholder: string
    }> = [
        { key: 'name', label: '이름', type: 'text', autoComplete: 'name', placeholder: '이름을 입력하세요' },
        { key: 'phone', label: '휴대전화 번호', type: 'tel', autoComplete: 'tel', placeholder: '01012345678' },
        { key: 'password', label: '비밀번호', type: 'password', autoComplete: 'new-password', placeholder: '8자 이상 입력하세요' },
        { key: 'passwordConfirmation', label: '비밀번호 확인', type: 'password', autoComplete: 'new-password', placeholder: '비밀번호를 다시 입력하세요' },
    ]

    return (
        <AuthPageLayout eyebrow="JOIN YMALL" title="취향의 시작을 함께해요." description="기본 정보를 입력하고 YMall 회원이 되어보세요." asideEyebrow="YMALL MEMBERS" asideTitle={<>FIND YOUR<br />OWN TASTE.</>} asideClassName="bg-[radial-gradient(circle_at_25%_24%,rgba(217,255,67,.95),transparent_22%),linear-gradient(145deg,#d9ddc8,#f1f0e8_58%,#c8cfab)]">
                <form className="grid gap-5" onSubmit={handleSubmit}>
                    <AuthField id="signup-email" label="이메일" type="email" value={form.email} onChange={(event) => updateField('email', event.target.value)} autoComplete="email" placeholder="you@example.com" maxLength={255} required
                        aria-invalid={currentEmailCheckStatus === 'unavailable' || currentEmailCheckStatus === 'error'} messageId="signup-email-message"
                        action={<button
                                className="h-10.5 border border-ink px-4 font-bold text-ink disabled:cursor-default disabled:border-line disabled:text-muted"
                                type="button"
                                onClick={handleEmailAvailabilityCheck}
                                disabled={!isEmailFormatValid || currentEmailCheckStatus === 'checking'}
                            >
                                {currentEmailCheckStatus === 'checking' ? '확인 중...' : '중복 확인'}
                            </button>}
                        message={hasEmailCheckMessage ? <>
                        {currentEmailCheckStatus === 'available' && (
                            <span className="font-medium text-[#657617]" role="status">사용 가능한 이메일입니다.</span>
                        )}
                        {currentEmailCheckStatus === 'unavailable' && (
                            <span className="font-medium text-[#b23b2f]" role="alert">이미 사용 중인 이메일입니다.</span>
                        )}
                        {currentEmailCheckStatus === 'error' && (
                            <span className="font-medium text-[#b23b2f]" role="alert">이메일 확인에 실패했습니다. 다시 시도해 주세요.</span>
                        )}
                        </> : undefined}
                    />
                    {fields.map((field) => (
                        <AuthField
                                key={field.key}
                                id={`signup-${field.key}`}
                                label={field.label}
                                type={field.type}
                                value={form[field.key]}
                                onChange={(event) => updateField(field.key, event.target.value)}
                                autoComplete={field.autoComplete}
                                placeholder={field.placeholder}
                                minLength={field.key === 'password' || field.key === 'passwordConfirmation' ? 8 : undefined}
                                maxLength={field.key === 'email' ? 255 : field.key === 'name' ? 50 : field.key === 'phone' ? 13 : 64}
                                pattern={field.key === 'phone' ? '01[016789]-?[0-9]{3,4}-?[0-9]{4}' : undefined}
                                required
                                aria-invalid={field.key === 'passwordConfirmation' && hasPasswordConfirmation && !isPasswordMatched}
                                messageId={field.key === 'passwordConfirmation' ? 'signup-password-confirmation-message' : undefined}
                                message={field.key === 'passwordConfirmation' && hasPasswordConfirmation ? (
                                <span
                                    className={`font-medium ${isPasswordMatched ? 'text-[#657617]' : 'text-[#b23b2f]'}`}
                                    role={isPasswordMatched ? 'status' : 'alert'}
                                >
                                    {isPasswordMatched ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.'}
                                </span>
                            ) : undefined}
                        />
                    ))}
                    {errorMessage && <AuthMessage tone="error">{errorMessage}</AuthMessage>}
                    <button className="mt-2 h-13.5 border border-ink bg-ink font-extrabold text-white disabled:cursor-default disabled:opacity-60" type="submit" disabled={!canSubmit}>
                        {isSubmitting ? '가입 중...' : '회원가입'}
                    </button>
                </form>
                <p className="mt-7 text-sm text-muted">
                    이미 회원이신가요?{' '}
                    <Link className="font-bold text-ink underline underline-offset-4" to="/login">
                        로그인
                    </Link>
                </p>
        </AuthPageLayout>
    )
}
