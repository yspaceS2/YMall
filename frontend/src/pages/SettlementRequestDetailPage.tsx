import { ArrowLeft, Check, LoaderCircle, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import {
    approveAdminSettlementRequest,
    completeAdminMockSettlementPayment,
    getAdminSettlementRequest,
    getAdminSettlementRequestHistories,
    rejectAdminSettlementRequest,
} from '../api/admin'
import { ApiError } from '../api/client'
import { getSettlementRequest } from '../api/seller'
import { settlementStatusLabel, settlementStatusTone } from '../components/settlement/settlementStatus'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { StatusBadge } from '../components/ui/StatusBadge'
import type { SettlementRequest, SettlementRequestHistory } from '../types/seller'
import { formatKoreanDateTime } from '../utils/dateTime'
import { formatPrice } from '../utils/product'
import { useOptionalAdminAuthorization } from '../auth/useAdminAuthorization'

export function SettlementRequestDetailPage({ role }: { role: 'seller' | 'admin' }) {
    const adminAuthorization = useOptionalAdminAuthorization()
    const canApprove = role === 'admin'
        && adminAuthorization?.hasPermission('SETTLEMENT_APPROVE') === true
    const { settlementRequestId: idParam } = useParams()
    const settlementRequestId = Number(idParam)
    const hasInvalidRequestId = !Number.isSafeInteger(settlementRequestId)
        || settlementRequestId <= 0
    const [request, setRequest] = useState<SettlementRequest | null>(null)
    const [histories, setHistories] = useState<SettlementRequestHistory[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [isProcessing, setIsProcessing] = useState(false)
    const [isRejecting, setIsRejecting] = useState(false)
    const [rejectionReason, setRejectionReason] = useState('')
    const [successMessage, setSuccessMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        if (hasInvalidRequestId) return
        const controller = new AbortController()
        const load = role === 'admin'
            ? Promise.all([
                getAdminSettlementRequest(settlementRequestId, controller.signal),
                getAdminSettlementRequestHistories(
                    settlementRequestId,
                    controller.signal,
                ),
            ])
            : Promise.all([
                getSettlementRequest(settlementRequestId, controller.signal),
                Promise.resolve([] as SettlementRequestHistory[]),
            ])
        load
            .then(([settlementRequest, settlementHistories]) => {
                setRequest(settlementRequest)
                setHistories(settlementHistories)
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError
                    ? error.message
                    : '정산 상세 정보를 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [hasInvalidRequestId, role, settlementRequestId])

    async function process(
        action: () => Promise<SettlementRequest>,
        successMessage: string,
    ) {
        setIsProcessing(true)
        setSuccessMessage('')
        setErrorMessage('')
        try {
            const updated = await action()
            setRequest(updated)
            if (role === 'admin') {
                setHistories(await getAdminSettlementRequestHistories(
                    settlementRequestId,
                ))
            }
            setIsRejecting(false)
            setRejectionReason('')
            setSuccessMessage(successMessage)
        } catch (error) {
            setErrorMessage(error instanceof ApiError
                ? error.message
                : '정산 상태를 변경하지 못했습니다.')
        } finally {
            setIsProcessing(false)
        }
    }

    return (
        <section className="mx-auto max-w-300 px-4 py-10 min-[601px]:px-8 min-[601px]:py-14">
            <Link
                className="mb-8 inline-flex items-center gap-2 text-sm font-bold text-muted hover:text-ink"
                to={`/${role}/settlement`}
            >
                <ArrowLeft className="size-4" aria-hidden="true" />
                정산 목록으로
            </Link>

            {successMessage && (
                <FeedbackMessage className="mb-6" tone="success">
                    {successMessage}
                </FeedbackMessage>
            )}
            {request && errorMessage && (
                <FeedbackMessage className="mb-6" tone="error">
                    {errorMessage}
                </FeedbackMessage>
            )}

            {hasInvalidRequestId ? (
                <p className="border border-danger/35 bg-danger-soft p-4 text-sm text-danger" role="alert">
                    올바르지 않은 정산번호입니다.
                </p>
            ) : isLoading ? (
                <div className="grid min-h-64 place-content-center">
                    <LoaderCircle className="size-6 animate-spin" aria-label="정산 상세 로딩 중" />
                </div>
            ) : errorMessage && !request ? (
                <p className="border border-danger/35 bg-danger-soft p-4 text-sm text-danger" role="alert">
                    {errorMessage}
                </p>
            ) : request && (
                <>
                    <header className="mb-8 border-b-2 border-ink pb-6">
                        <p className="text-[11px] font-extrabold tracking-[.18em] text-accent">
                            SETTLEMENT DETAIL
                        </p>
                        <div className="mt-2 flex flex-wrap items-end justify-between gap-4">
                            <div>
                                <h1 className="font-serif text-[clamp(36px,5vw,56px)] leading-none tracking-tighter">
                                    정산 #{request.settlementRequestId}
                                </h1>
                                <p className="mt-3 text-sm text-muted">
                                    {request.storeName} · 신청 {formatKoreanDateTime(request.createdAt)}
                                </p>
                            </div>
                            <StatusBadge className="px-3 py-2 text-xs" tone={settlementStatusTone[request.status]}>
                                {settlementStatusLabel[request.status]}
                            </StatusBadge>
                        </div>
                    </header>

                    <section className="border-y-2 border-ink">
                        <DetailRow label="매출 금액" value={formatPrice(request.grossAmount)} />
                        <DetailRow label="정산 수수료" value={formatPrice(request.feeAmount)} />
                        <DetailRow label="지급 예정액" value={formatPrice(request.settlementAmount)} strong />
                        <DetailRow label="신청일" value={formatKoreanDateTime(request.createdAt)} />
                        <DetailRow label="검토일" value={formatOptionalDate(request.reviewedAt)} />
                        <DetailRow label="지급 처리일" value={formatOptionalDate(request.paidAt)} />
                        {request.mockPaymentReference && (
                            <DetailRow label="지급 처리번호" value={request.mockPaymentReference} />
                        )}
                        {request.rejectionReason && (
                            <DetailRow label="반려 사유" value={request.rejectionReason} danger />
                        )}
                    </section>

                    {request.status === 'PAID' && (
                        <p className="mt-5 border border-success/35 bg-success-soft p-4 text-sm text-success">
                            포트폴리오용 모의 지급이며 실제 계좌 이체는 발생하지 않았습니다.
                        </p>
                    )}

                    {role === 'admin' && (
                        <section className="mt-8 border-t-2 border-ink pt-5">
                            <h2 className="text-lg font-bold">처리 이력</h2>
                            {histories.length === 0 ? (
                                <p className="mt-4 text-sm text-muted">
                                    기록된 처리 이력이 없습니다.
                                </p>
                            ) : (
                                <ol className="mt-4 grid gap-px border-y border-line bg-line">
                                    {histories.map((history, index) => (
                                        <li
                                            className="grid gap-2 bg-surface px-4 py-4 min-[701px]:grid-cols-[180px_1fr_auto] min-[701px]:items-center"
                                            key={`${history.createdAt}-${index}`}
                                        >
                                            <span className="text-xs text-muted">
                                                {formatKoreanDateTime(history.createdAt)}
                                            </span>
                                            <span className="text-sm font-bold">
                                                {history.fromStatus
                                                    ? settlementStatusLabel[history.fromStatus]
                                                    : '신규 신청'}
                                                {' → '}
                                                {settlementStatusLabel[history.toStatus]}
                                            </span>
                                            <span className="text-xs text-muted">
                                                {history.actorName}
                                            </span>
                                            {history.reason && (
                                                <p className="text-sm text-danger min-[701px]:col-span-3">
                                                    사유: {history.reason}
                                                </p>
                                            )}
                                        </li>
                                    ))}
                                </ol>
                            )}
                        </section>
                    )}

                    {canApprove && request.status === 'REQUESTED' && (
                        <section className="mt-8 border-t-2 border-ink pt-5">
                            <h2 className="text-lg font-bold">정산 검토</h2>
                            {isRejecting && (
                                <textarea
                                    className="mt-4 min-h-24 w-full border border-line bg-surface p-3 text-sm text-ink"
                                    maxLength={500}
                                    placeholder="반려 사유를 입력하세요."
                                    value={rejectionReason}
                                    onChange={(event) => setRejectionReason(event.target.value)}
                                />
                            )}
                            <div className="mt-4 flex flex-wrap gap-2">
                                <button
                                    className="flex h-11 items-center gap-2 bg-ink px-5 text-xs font-bold text-surface disabled:opacity-50"
                                    type="button"
                                    disabled={isProcessing}
                                    onClick={() => void process(
                                        () => approveAdminSettlementRequest(request.settlementRequestId),
                                        '정산 신청을 승인했습니다.',
                                    )}
                                >
                                    <Check className="size-4" /> 승인
                                </button>
                                <button
                                    className="flex h-11 items-center gap-2 border border-danger px-5 text-xs font-bold text-danger disabled:opacity-50"
                                    type="button"
                                    disabled={isProcessing}
                                    onClick={() => {
                                        if (!isRejecting) {
                                            setIsRejecting(true)
                                            return
                                        }
                                        if (!rejectionReason.trim()) {
                                            setErrorMessage('반려 사유를 입력해 주세요.')
                                            return
                                        }
                                        void process(
                                            () => rejectAdminSettlementRequest(
                                                request.settlementRequestId,
                                                rejectionReason,
                                            ),
                                            '정산 신청을 반려했습니다.',
                                        )
                                    }}
                                >
                                    <X className="size-4" /> 반려
                                </button>
                            </div>
                        </section>
                    )}

                    {canApprove && request.status === 'APPROVED' && (
                        <button
                            className="mt-8 h-11 bg-accent px-5 text-xs font-bold text-paper disabled:opacity-50"
                            type="button"
                            disabled={isProcessing}
                            onClick={() => void process(
                                () => completeAdminMockSettlementPayment(request.settlementRequestId),
                                '모의 지급 처리를 완료했습니다.',
                            )}
                        >
                            모의 지급 완료 처리
                        </button>
                    )}
                </>
            )}
        </section>
    )
}

function DetailRow({
    label,
    value,
    strong = false,
    danger = false,
}: {
    label: string
    value: string
    strong?: boolean
    danger?: boolean
}) {
    return (
        <div className="grid gap-2 border-b border-line px-4 py-4 last:border-b-0 min-[701px]:grid-cols-[180px_1fr] min-[701px]:items-center">
            <span className="text-xs font-bold text-muted">{label}</span>
            <span className={[
                strong ? 'text-lg font-extrabold' : 'text-sm',
                danger ? 'text-danger' : '',
            ].join(' ')}>
                {value}
            </span>
        </div>
    )
}

function formatOptionalDate(value: string | null) {
    return value ? formatKoreanDateTime(value) : '-'
}
