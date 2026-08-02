import { LoaderCircle, Store } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import {
    createSellerApplication,
    getMySellerApplication,
} from '../../api/sellerApplications'
import { ApiError } from '../../api/client'
import { useAuth } from '../../auth/useAuth'
import type { SellerApplication } from '../../types/sellerApplication'
import { formatKoreanDateTime } from '../../utils/dateTime'
import { FeedbackMessage } from '../ui/FeedbackMessage'

const statusLabels = {
    PENDING: '심사 대기',
    APPROVED: '승인 완료',
    REJECTED: '반려',
} as const

export function SellerApplicationPanel() {
    const { logout, role } = useAuth()
    const [application, setApplication] = useState<SellerApplication | null>(null)
    const [storeName, setStoreName] = useState('')
    const [businessNumber, setBusinessNumber] = useState('')
    const [description, setDescription] = useState('')
    const [isLoading, setIsLoading] = useState(true)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        if (role !== 'ROLE_USER') {
            return
        }
        const controller = new AbortController()
        getMySellerApplication(controller.signal)
            .then((response) => {
                setApplication(response)
                setStoreName(response.storeName)
                setBusinessNumber(response.businessNumber)
                setDescription(response.description ?? '')
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                if (error instanceof ApiError && error.code === 'SELLER_APPLICATION_NOT_FOUND') {
                    return
                }
                setErrorMessage(
                    error instanceof ApiError
                        ? error.message
                        : '판매자 신청 정보를 불러오지 못했습니다.',
                )
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [role])

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setErrorMessage('')
        setIsSubmitting(true)
        try {
            const response = await createSellerApplication({
                storeName: storeName.trim(),
                businessNumber: businessNumber.trim(),
                description: description.trim(),
            })
            setApplication(response)
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError
                    ? error.message
                    : '판매자 신청을 접수하지 못했습니다.',
            )
        } finally {
            setIsSubmitting(false)
        }
    }

    if (role === 'ROLE_SELLER') {
        return <Navigate to="/seller" replace />
    }

    if (role === 'ROLE_ADMIN') {
        return <Navigate to="/admin" replace />
    }

    if (isLoading) {
        return (
            <div className="grid min-h-64 place-content-center border border-line bg-surface">
                <LoaderCircle className="size-6 animate-spin" aria-label="판매자 신청 정보 로딩 중" />
            </div>
        )
    }

    const canApply = application === null || application.status === 'REJECTED'

    return (
        <div className="mt-8 grid gap-6 min-[901px]:grid-cols-[minmax(0,1.2fr)_minmax(280px,.8fr)]">
            <section className="border border-line bg-surface p-6 min-[601px]:p-8">
                <p className="text-[11px] font-extrabold tracking-[.16em] text-muted">
                    SELLER APPLICATION
                </p>
                <h2 className="mt-2 font-serif text-3xl">판매자 신청</h2>
                <p className="mt-3 text-sm leading-7 text-muted">
                    상점과 사업자 정보를 제출하면 관리자가 확인한 뒤 판매자 권한을 부여합니다.
                </p>

                {errorMessage && (
                    <FeedbackMessage className="mt-5" tone="error">
                        {errorMessage}
                    </FeedbackMessage>
                )}

                {application?.status === 'PENDING' && (
                    <FeedbackMessage className="mt-5" tone="info">
                        신청이 접수되어 관리자 심사를 기다리고 있습니다.
                    </FeedbackMessage>
                )}
                {application?.status === 'APPROVED' && (
                    <div className="mt-5 grid gap-4">
                        <FeedbackMessage tone="success">
                            판매자 신청이 승인되었습니다. 새 권한을 적용하려면 다시 로그인해 주세요.
                        </FeedbackMessage>
                        <button
                            className="h-12 bg-ink px-5 text-sm font-extrabold text-white"
                            type="button"
                            onClick={() => void logout()}
                        >
                            로그아웃하고 다시 로그인
                        </button>
                    </div>
                )}
                {application?.status === 'REJECTED' && (
                    <FeedbackMessage className="mt-5" tone="error">
                        반려 사유: {application.rejectionReason}
                    </FeedbackMessage>
                )}

                {canApply && (
                    <form className="mt-7 grid gap-5" onSubmit={handleSubmit}>
                        <label className="grid gap-2 text-xs font-bold text-muted">
                            <span>상점명</span>
                            <input
                                className="border-0 border-b border-line bg-transparent px-0.5 py-3.5 text-ink outline-0 focus:border-ink"
                                value={storeName}
                                onChange={(event) => setStoreName(event.target.value)}
                                maxLength={100}
                                required
                            />
                        </label>
                        <label className="grid gap-2 text-xs font-bold text-muted">
                            <span>사업자등록번호</span>
                            <input
                                className="border-0 border-b border-line bg-transparent px-0.5 py-3.5 text-ink outline-0 focus:border-ink"
                                value={businessNumber}
                                onChange={(event) => setBusinessNumber(event.target.value)}
                                placeholder="123-45-67890"
                                pattern="[0-9]{3}-?[0-9]{2}-?[0-9]{5}"
                                maxLength={12}
                                inputMode="numeric"
                                required
                            />
                        </label>
                        <label className="grid gap-2 text-xs font-bold text-muted">
                            <span>상점 소개</span>
                            <textarea
                                className="min-h-32 resize-y border border-line bg-transparent p-3 text-sm text-ink outline-0 focus:border-ink"
                                value={description}
                                onChange={(event) => setDescription(event.target.value)}
                                maxLength={2000}
                            />
                        </label>
                        <button
                            className="h-12 bg-ink px-5 text-sm font-extrabold text-white disabled:opacity-60"
                            type="submit"
                            disabled={isSubmitting}
                        >
                            {isSubmitting
                                ? '신청 중...'
                                : application
                                    ? '판매자 재신청'
                                    : '판매자 신청'}
                        </button>
                    </form>
                )}
            </section>

            <aside className="border border-line bg-paper p-6 min-[601px]:p-8">
                <Store className="size-7 text-accent" aria-hidden="true" />
                <h3 className="mt-4 text-lg font-bold">신청 처리 안내</h3>
                <ol className="mt-5 grid gap-4 text-sm leading-6 text-muted">
                    <li><b className="text-ink">1. 신청</b><br />상점명과 사업자 정보를 제출합니다.</li>
                    <li><b className="text-ink">2. 관리자 심사</b><br />등록 정보의 중복 여부와 내용을 확인합니다.</li>
                    <li><b className="text-ink">3. 권한 적용</b><br />승인 후 다시 로그인하면 판매자 센터를 이용할 수 있습니다.</li>
                </ol>
                {application && (
                    <dl className="mt-7 grid gap-3 border-t border-line pt-5 text-sm">
                        <div className="flex justify-between gap-4">
                            <dt className="text-muted">현재 상태</dt>
                            <dd className="font-bold">{statusLabels[application.status]}</dd>
                        </div>
                        <div className="flex justify-between gap-4">
                            <dt className="text-muted">신청일</dt>
                            <dd>{formatKoreanDateTime(application.createdAt)}</dd>
                        </div>
                    </dl>
                )}
            </aside>
        </div>
    )
}
