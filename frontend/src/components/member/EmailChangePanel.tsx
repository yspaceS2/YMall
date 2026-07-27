import { useEffect, useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
    changeMemberEmail,
    getOAuthAuthorizationUrl,
    requestNewEmailVerification,
    startEmailChangeOAuthReauthentication,
    startEmailChangeReauthentication,
} from '../../api/auth'
import { ApiError } from '../../api/client'
import { useAuth } from '../../auth/useAuth'
import type { OAuthProvider } from '../../types/auth'
import { FeedbackMessage } from '../ui/FeedbackMessage'

type Stage = 'reauthentication' | 'newEmail' | 'newCode'

interface EmailChangePanelProps {
    currentEmail: string
    hasPassword: boolean
    linkedProviders: OAuthProvider[]
}

interface EmailChangeLocationState {
    emailChangeReauthenticated?: boolean
    emailChangeReauthenticationError?: string
}

export function EmailChangePanel({
    currentEmail,
    hasPassword,
    linkedProviders,
}: EmailChangePanelProps) {
    const navigate = useNavigate()
    const location = useLocation()
    const { logout } = useAuth()
    const locationState = location.state as EmailChangeLocationState | null
    const [stage, setStage] = useState<Stage>(
        locationState?.emailChangeReauthenticated ? 'newEmail' : 'reauthentication',
    )
    const [currentPassword, setCurrentPassword] = useState('')
    const [newEmail, setNewEmail] = useState('')
    const [newRequestId, setNewRequestId] = useState('')
    const [newCode, setNewCode] = useState('')
    const [message, setMessage] = useState(
        locationState?.emailChangeReauthenticated ? '소셜 계정 본인 확인이 완료되었습니다.' : '',
    )
    const [error, setError] = useState(locationState?.emailChangeReauthenticationError ?? '')
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [reauthenticatingProvider, setReauthenticatingProvider] =
        useState<OAuthProvider | null>(null)

    useEffect(() => {
        if (locationState?.emailChangeReauthenticated
            || locationState?.emailChangeReauthenticationError) {
            navigate('/mypage', { replace: true, state: null })
        }
    }, [
        locationState?.emailChangeReauthenticated,
        locationState?.emailChangeReauthenticationError,
        navigate,
    ])

    function reportError(value: unknown, fallback: string) {
        setError(value instanceof ApiError ? value.message : fallback)
    }

    async function startReauthentication() {
        setError('')
        setMessage('')
        setIsSubmitting(true)
        try {
            await startEmailChangeReauthentication(currentPassword)
            setStage('newEmail')
            setMessage('본인 확인이 완료되었습니다.')
        } catch (value) {
            reportError(value, '본인 확인을 시작하지 못했습니다.')
        } finally {
            setIsSubmitting(false)
        }
    }

    async function startOAuthReauthentication(provider: OAuthProvider) {
        setError('')
        setMessage('')
        setReauthenticatingProvider(provider)
        setIsSubmitting(true)
        try {
            await startEmailChangeOAuthReauthentication(provider)
            window.location.assign(getOAuthAuthorizationUrl(provider))
        } catch (value) {
            reportError(value, '소셜 계정 본인 확인을 시작하지 못했습니다.')
            setReauthenticatingProvider(null)
            setIsSubmitting(false)
        }
    }

    async function sendNewEmailCode(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError('')
        setMessage('')
        setIsSubmitting(true)
        try {
            const normalizedEmail = newEmail.trim().toLowerCase()
            const response = await requestNewEmailVerification(normalizedEmail)
            setNewEmail(normalizedEmail)
            setNewRequestId(response.requestId)
            setStage('newCode')
            setMessage(`${normalizedEmail}로 인증번호를 발송했습니다.`)
        } catch (value) {
            reportError(value, '새 이메일 인증번호를 발송하지 못했습니다.')
        } finally {
            setIsSubmitting(false)
        }
    }

    async function completeChange(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError('')
        setMessage('')
        setIsSubmitting(true)
        try {
            await changeMemberEmail(newRequestId, newEmail, newCode)
            await logout()
            navigate('/login', {
                replace: true,
                state: { emailChanged: true },
            })
        } catch (value) {
            reportError(value, '이메일을 변경하지 못했습니다.')
            setIsSubmitting(false)
        }
    }

    return (
        <section className="grid content-start gap-5 border border-line bg-surface p-6 min-[601px]:p-8" aria-labelledby="email-change-title">
            <div>
                <p className="text-[11px] font-extrabold tracking-[.16em] text-muted">LOGIN EMAIL</p>
                <h2 className="mt-2 font-serif text-3xl" id="email-change-title">이메일 변경</h2>
                <p className="mt-3 text-sm leading-6 text-muted">
                    현재 이메일은 <strong className="text-ink">{currentEmail}</strong>입니다.
                </p>
            </div>

            {stage === 'reauthentication' && (
                <div className="grid gap-4">
                    {hasPassword ? (
                        <label className="grid gap-2 text-xs font-bold text-muted">
                            <span>현재 비밀번호</span>
                            <input
                                className="border-0 border-b border-line bg-transparent px-0.5 py-3.5 text-ink outline-0 focus:border-ink"
                                type="password"
                                value={currentPassword}
                                onChange={(event) => setCurrentPassword(event.target.value)}
                                autoComplete="current-password"
                                maxLength={64}
                            />
                        </label>
                    ) : (
                        <>
                            <p className="text-sm leading-6 text-muted">
                                이메일 변경 전, 현재 회원에 연결된 소셜 계정으로 다시 로그인해 본인 여부를 확인합니다.
                            </p>
                            <div className="grid gap-3 min-[601px]:grid-cols-3">
                                {linkedProviders.map((provider) => (
                                    <button
                                        className="h-12 border border-line bg-surface px-3 text-sm font-bold text-ink disabled:opacity-60"
                                        type="button"
                                        key={provider}
                                        onClick={() => startOAuthReauthentication(provider)}
                                        disabled={isSubmitting}
                                    >
                                        {reauthenticatingProvider === provider
                                            ? '이동 중...'
                                            : `${provider === 'GOOGLE'
                                                ? 'Google'
                                                : provider === 'KAKAO'
                                                    ? '카카오'
                                                    : '네이버'}로 본인 확인`}
                                    </button>
                                ))}
                            </div>
                        </>
                    )}
                    {hasPassword && (
                        <button
                            className="h-12 border border-ink bg-ink font-extrabold text-white disabled:opacity-60"
                            type="button"
                            onClick={startReauthentication}
                            disabled={isSubmitting || currentPassword.length === 0}
                        >
                            {isSubmitting ? '확인 중...' : '현재 비밀번호로 본인 확인'}
                        </button>
                    )}
                </div>
            )}

            {stage === 'newEmail' && (
                <form className="grid gap-4" onSubmit={sendNewEmailCode}>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>새 이메일</span>
                        <input
                            className="border-0 border-b border-line bg-transparent px-0.5 py-3.5 text-ink outline-0 focus:border-ink"
                            type="email"
                            value={newEmail}
                            onChange={(event) => setNewEmail(event.target.value)}
                            autoComplete="email"
                            maxLength={255}
                            required
                        />
                    </label>
                    <button className="h-12 border border-ink bg-ink font-extrabold text-white disabled:opacity-60" type="submit" disabled={isSubmitting || newEmail.trim().length === 0}>
                        {isSubmitting ? '발송 중...' : '새 이메일 인증번호 발송'}
                    </button>
                </form>
            )}

            {stage === 'newCode' && (
                <form className="grid gap-4" onSubmit={completeChange}>
                    <label className="grid gap-2 text-xs font-bold text-muted">
                        <span>{newEmail} 인증번호</span>
                        <input
                            className="border-0 border-b border-line bg-transparent px-0.5 py-3.5 text-ink outline-0 focus:border-ink"
                            value={newCode}
                            onChange={(event) => setNewCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                            inputMode="numeric"
                            pattern="\d{6}"
                            maxLength={6}
                            required
                        />
                    </label>
                    <p className="text-xs leading-5 text-muted">
                        변경 완료 후 모든 로그인 세션이 종료되며 새 이메일로 다시 로그인해야 합니다.
                    </p>
                    <button className="h-12 border border-ink bg-ink font-extrabold text-white disabled:opacity-60" type="submit" disabled={isSubmitting || newCode.length !== 6}>
                        {isSubmitting ? '변경 중...' : '이메일 변경 완료'}
                    </button>
                </form>
            )}

            {message && <FeedbackMessage tone="success">{message}</FeedbackMessage>}
            {error && <FeedbackMessage tone="error">{error}</FeedbackMessage>}
        </section>
    )
}
