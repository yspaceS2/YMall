import { Check, LoaderCircle, Store, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import {
    getAdminSellerApplications,
    reviewSellerApplication,
} from '../../api/sellerApplications'
import { ApiError } from '../../api/client'
import type { SellerApplication } from '../../types/sellerApplication'
import { formatKoreanDateTime } from '../../utils/dateTime'
import { FeedbackMessage } from '../ui/FeedbackMessage'
import { useAdminAuthorization } from '../../auth/useAdminAuthorization'

export function AdminSellerApplicationPanel() {
    const { hasPermission } = useAdminAuthorization()
    const canDecide = hasPermission('SELLER_APPLICATION_DECIDE')
    const [applications, setApplications] = useState<SellerApplication[]>([])
    const [rejectionReasons, setRejectionReasons] = useState<Record<number, string>>({})
    const [processingId, setProcessingId] = useState<number | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [message, setMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        getAdminSellerApplications('PENDING', { signal: controller.signal })
            .then((response) => setApplications(response.content))
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(
                    error instanceof ApiError
                        ? error.message
                        : '판매자 신청 목록을 불러오지 못했습니다.',
                )
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [])

    async function review(
        application: SellerApplication,
        status: 'APPROVED' | 'REJECTED',
    ) {
        const rejectionReason = rejectionReasons[application.sellerApplicationId]?.trim()
        if (status === 'REJECTED' && !rejectionReason) {
            setErrorMessage('반려하려면 반려 사유를 입력해 주세요.')
            return
        }

        setProcessingId(application.sellerApplicationId)
        setMessage('')
        setErrorMessage('')
        try {
            await reviewSellerApplication(
                application.sellerApplicationId,
                status,
                rejectionReason,
            )
            setApplications((current) => current.filter(
                (item) => item.sellerApplicationId !== application.sellerApplicationId,
            ))
            setMessage(
                `'${application.storeName}' 판매자 신청을 ${status === 'APPROVED' ? '승인' : '반려'}했습니다.`,
            )
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError
                    ? error.message
                    : '판매자 신청을 처리하지 못했습니다.',
            )
        } finally {
            setProcessingId(null)
        }
    }

    if (isLoading) {
        return (
            <div className="grid min-h-64 place-content-center">
                <LoaderCircle className="size-6 animate-spin" aria-label="판매자 신청 로딩 중" />
            </div>
        )
    }

    return (
        <section className="min-w-0 border-t-2 border-ink pt-5">
            <h2 className="mb-6 flex items-center gap-2 text-xl font-bold">
                <Store className="size-5" aria-hidden="true" />
                판매자 신청 관리
            </h2>
            {message && <FeedbackMessage className="mb-5" tone="success">{message}</FeedbackMessage>}
            {errorMessage && <FeedbackMessage className="mb-5" tone="error">{errorMessage}</FeedbackMessage>}
            {applications.length === 0 ? (
                <p className="text-sm text-muted">심사를 기다리는 판매자 신청이 없습니다.</p>
            ) : (
                <div className="grid gap-4">
                    {applications.map((application) => (
                        <article
                            className={canDecide
                                ? 'grid gap-5 border border-line bg-surface p-5 min-[901px]:grid-cols-[minmax(0,1fr)_minmax(280px,.55fr)]'
                                : 'border border-line bg-surface p-5'}
                            key={application.sellerApplicationId}
                        >
                            <div>
                                <div className="flex flex-wrap items-center gap-2">
                                    <strong className="text-lg">{application.storeName}</strong>
                                    <span className="bg-success-soft px-2.5 py-1 text-[11px] font-bold text-success">
                                        심사 대기
                                    </span>
                                </div>
                                <dl className="mt-4 grid gap-2 text-sm">
                                    <div className="flex flex-wrap gap-2">
                                        <dt className="w-24 text-muted">신청 회원</dt>
                                        <dd>{application.memberName} · {application.memberEmail}</dd>
                                    </div>
                                    <div className="flex flex-wrap gap-2">
                                        <dt className="w-24 text-muted">사업자번호</dt>
                                        <dd>{application.businessNumber}</dd>
                                    </div>
                                    <div className="flex flex-wrap gap-2">
                                        <dt className="w-24 text-muted">신청일</dt>
                                        <dd>{formatKoreanDateTime(application.createdAt)}</dd>
                                    </div>
                                </dl>
                                {application.description && (
                                    <p className="mt-4 border-l-2 border-line pl-4 text-sm leading-6 text-muted">
                                        {application.description}
                                    </p>
                                )}
                            </div>
                            {canDecide && <div className="grid content-start gap-3">
                                <button
                                    className="flex h-11 items-center justify-center gap-2 bg-ink px-4 text-xs font-bold text-white disabled:opacity-50"
                                    type="button"
                                    disabled={processingId !== null}
                                    onClick={() => void review(application, 'APPROVED')}
                                >
                                    <Check className="size-4" aria-hidden="true" />
                                    승인
                                </button>
                                <label className="grid gap-2 text-xs font-bold text-muted">
                                    <span>반려 사유</span>
                                    <textarea
                                        className="min-h-20 resize-y border border-line bg-transparent p-3 text-sm font-normal text-ink outline-0 focus:border-ink"
                                        value={rejectionReasons[application.sellerApplicationId] ?? ''}
                                        onChange={(event) => setRejectionReasons((current) => ({
                                            ...current,
                                            [application.sellerApplicationId]: event.target.value,
                                        }))}
                                        maxLength={500}
                                    />
                                </label>
                                <button
                                    className="flex h-11 items-center justify-center gap-2 border border-danger px-4 text-xs font-bold text-danger disabled:opacity-50"
                                    type="button"
                                    disabled={processingId !== null}
                                    onClick={() => void review(application, 'REJECTED')}
                                >
                                    <X className="size-4" aria-hidden="true" />
                                    반려
                                </button>
                            </div>}
                        </article>
                    ))}
                </div>
            )}
        </section>
    )
}
