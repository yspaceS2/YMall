import { useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { confirmPasswordReset, requestPasswordReset, verifyPasswordReset } from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { AuthField } from '../components/auth/AuthField'
import { AuthMessage } from '../components/auth/AuthMessage'
import { AuthPageLayout } from '../components/auth/AuthPageLayout'

type PasswordResetStep = 'email' | 'verification' | 'password' | 'complete'

const stepLabels = ['이메일 확인', '인증번호', '새 비밀번호']

export function PasswordResetPage() {
    const { isAuthenticated } = useAuth()
    const [step, setStep] = useState<PasswordResetStep>('email')
    const [email, setEmail] = useState('')
    const [requestId, setRequestId] = useState('')
    const [verificationCode, setVerificationCode] = useState('')
    const [resetToken, setResetToken] = useState('')
    const [newPassword, setNewPassword] = useState('')
    const [newPasswordConfirmation, setNewPasswordConfirmation] = useState('')
    const [errorMessage, setErrorMessage] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)

    if (isAuthenticated) {
        return <Navigate to="/mypage" replace />
    }

    const currentStepIndex = step === 'email' ? 0 : step === 'verification' ? 1 : 2
    const hasPasswordConfirmation = newPasswordConfirmation.length > 0
    const isPasswordMatched = hasPasswordConfirmation && newPassword === newPasswordConfirmation

    async function handleEmailSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setErrorMessage('')
        setIsSubmitting(true)
        try {
            const response = await requestPasswordReset(email.trim())
            setRequestId(response.requestId)
            setStep('verification')
        } catch (error) {
            setErrorMessage(toErrorMessage(error))
        } finally {
            setIsSubmitting(false)
        }
    }

    async function handleVerificationSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setErrorMessage('')
        setIsSubmitting(true)
        try {
            const response = await verifyPasswordReset(requestId, verificationCode)
            setResetToken(response.resetToken)
            setVerificationCode('')
            setStep('password')
        } catch (error) {
            setErrorMessage(toErrorMessage(error))
        } finally {
            setIsSubmitting(false)
        }
    }

    async function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setErrorMessage('')
        if (!isPasswordMatched) {
            setErrorMessage('새 비밀번호 확인이 일치하지 않습니다.')
            return
        }

        setIsSubmitting(true)
        try {
            await confirmPasswordReset({
                resetToken,
                newPassword,
                newPasswordConfirmation,
            })
            setRequestId('')
            setResetToken('')
            setNewPassword('')
            setNewPasswordConfirmation('')
            setStep('complete')
        } catch (error) {
            setErrorMessage(toErrorMessage(error))
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <AuthPageLayout
            eyebrow="PASSWORD RECOVERY"
            title="비밀번호 찾기"
            description="가입한 이메일을 인증하고 새로운 비밀번호를 설정해 주세요."
            asideEyebrow="YMALL SECURITY"
            asideTitle={<>SAFE ACCESS,<br />SIMPLE RESET.</>}
            contentClassName="max-w-120"
        >
            {step !== 'complete' && (
                <ol className="mb-9 grid grid-cols-3 gap-2" aria-label="비밀번호 재설정 단계">
                    {stepLabels.map((label, index) => (
                        <li
                            key={label}
                            className={`border-t-2 pt-2 text-[11px] font-bold ${
                                index <= currentStepIndex ? 'border-ink text-ink' : 'border-line text-muted'
                            }`}
                        >
                            {index + 1}. {label}
                        </li>
                    ))}
                </ol>
            )}

            {step === 'email' && (
                <form className="grid gap-6" onSubmit={handleEmailSubmit}>
                    <AuthField
                        id="password-reset-email"
                        label="이메일"
                        type="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        autoComplete="email"
                        placeholder="you@example.com"
                        maxLength={255}
                        required
                    />
                    <AuthMessage tone="info">
                        가입 여부 보호를 위해 일반 회원 계정이 존재할 때만 메일이 발송됩니다.
                        소셜 전용 회원은 이용한 소셜 계정으로 로그인해 주세요.
                    </AuthMessage>
                    {errorMessage && <AuthMessage tone="error">{errorMessage}</AuthMessage>}
                    <SubmitButton isSubmitting={isSubmitting}>인증번호 받기</SubmitButton>
                </form>
            )}

            {step === 'verification' && (
                <form className="grid gap-6" onSubmit={handleVerificationSubmit}>
                    <AuthMessage tone="success">
                        가입된 일반 회원 계정이라면 인증번호를 전송했습니다.
                    </AuthMessage>
                    <AuthField
                        id="password-reset-code"
                        label="인증번호"
                        type="text"
                        inputMode="numeric"
                        value={verificationCode}
                        onChange={(event) => setVerificationCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                        autoComplete="one-time-code"
                        placeholder="6자리 숫자"
                        pattern="[0-9]{6}"
                        maxLength={6}
                        required
                    />
                    {errorMessage && <AuthMessage tone="error">{errorMessage}</AuthMessage>}
                    <SubmitButton isSubmitting={isSubmitting}>인증번호 확인</SubmitButton>
                    <button
                        className="text-sm font-bold text-muted underline underline-offset-4"
                        type="button"
                        onClick={() => {
                            setStep('email')
                            setRequestId('')
                            setVerificationCode('')
                            setErrorMessage('')
                        }}
                    >
                        이메일을 다시 입력하기
                    </button>
                </form>
            )}

            {step === 'password' && (
                <form className="grid gap-6" onSubmit={handlePasswordSubmit}>
                    <AuthField
                        id="password-reset-new-password"
                        label="새 비밀번호"
                        type="password"
                        value={newPassword}
                        onChange={(event) => setNewPassword(event.target.value)}
                        autoComplete="new-password"
                        placeholder="8자 이상 입력해 주세요"
                        minLength={8}
                        maxLength={64}
                        required
                    />
                    <AuthField
                        id="password-reset-new-password-confirmation"
                        label="새 비밀번호 확인"
                        type="password"
                        value={newPasswordConfirmation}
                        onChange={(event) => setNewPasswordConfirmation(event.target.value)}
                        autoComplete="new-password"
                        placeholder="새 비밀번호를 다시 입력해 주세요"
                        minLength={8}
                        maxLength={64}
                        required
                        aria-invalid={hasPasswordConfirmation && !isPasswordMatched}
                        messageId="password-reset-confirmation-message"
                        message={hasPasswordConfirmation ? (
                            <span
                                className={`font-medium ${isPasswordMatched ? 'text-success' : 'text-danger'}`}
                                role={isPasswordMatched ? 'status' : 'alert'}
                            >
                                {isPasswordMatched ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.'}
                            </span>
                        ) : undefined}
                    />
                    {errorMessage && <AuthMessage tone="error">{errorMessage}</AuthMessage>}
                    <SubmitButton isSubmitting={isSubmitting} disabled={!isPasswordMatched}>
                        비밀번호 재설정
                    </SubmitButton>
                </form>
            )}

            {step === 'complete' && (
                <div className="grid gap-6">
                    <AuthMessage tone="success">
                        비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해 주세요.
                    </AuthMessage>
                    <Link
                        className="grid h-13.5 place-items-center border border-ink bg-ink font-extrabold text-white"
                        to="/login"
                        replace
                    >
                        로그인으로 돌아가기
                    </Link>
                </div>
            )}

            {step !== 'complete' && (
                <p className="mt-7 text-sm text-muted">
                    로그인 페이지로 돌아가기 {' '}
                    <Link className="font-bold text-ink underline underline-offset-4" to="/login">
                        로그인
                    </Link>
                </p>
            )}
        </AuthPageLayout>
    )
}

interface SubmitButtonProps {
    children: string
    isSubmitting: boolean
    disabled?: boolean
}

function SubmitButton({ children, isSubmitting, disabled = false }: SubmitButtonProps) {
    return (
        <button
            className="mt-2 h-13.5 border border-ink bg-ink font-extrabold text-white disabled:cursor-wait disabled:opacity-60"
            type="submit"
            disabled={disabled || isSubmitting}
        >
            {isSubmitting ? '처리 중...' : children}
        </button>
    )
}

function toErrorMessage(error: unknown) {
    return error instanceof ApiError
        ? error.message
        : '요청 처리 중 오류가 발생했습니다. 다시 시도해 주세요.'
}
