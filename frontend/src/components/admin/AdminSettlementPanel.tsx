import { Check, CircleDollarSign, LoaderCircle, X } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'

import {
    approveAdminSettlementRequest,
    completeAdminMockSettlementPayment,
    getAdminSettlementRequests,
    rejectAdminSettlementRequest,
} from '../../api/admin'
import { ApiError } from '../../api/client'
import type {
    AdminSettlementRequest,
    SettlementRequestStatus,
} from '../../types/admin'
import { formatKoreanDateTime } from '../../utils/dateTime'
import { formatPrice } from '../../utils/product'
import { StatusBadge } from '../ui/StatusBadge'
import { settlementStatusTone } from '../settlement/settlementStatus'

const statusLabel: Record<SettlementRequestStatus, string> = {
    REQUESTED: '승인 대기',
    APPROVED: '지급 대기',
    REJECTED: '반려',
    PAID: '모의 지급 완료',
}

export function AdminSettlementPanel() {
    const [requests, setRequests] = useState<AdminSettlementRequest[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [processingId, setProcessingId] = useState<number | null>(null)
    const [rejectingId, setRejectingId] = useState<number | null>(null)
    const [rejectionReason, setRejectionReason] = useState('')
    const [message, setMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')

    const load = useCallback(async (signal?: AbortSignal) => {
        setRequests((await getAdminSettlementRequests({ signal })).content)
    }, [])

    useEffect(() => {
        const controller = new AbortController()
        getAdminSettlementRequests({ signal: controller.signal })
            .then((response) => setRequests(response.content))
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError
                    ? error.message
                    : '정산 신청을 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [load])

    async function process(
        requestId: number,
        action: () => Promise<AdminSettlementRequest>,
        successMessage: string,
    ) {
        setProcessingId(requestId)
        setMessage('')
        setErrorMessage('')
        try {
            await action()
            await load()
            setRejectingId(null)
            setRejectionReason('')
            setMessage(successMessage)
        } catch (error) {
            setErrorMessage(error instanceof ApiError
                ? error.message
                : '정산 상태를 변경하지 못했습니다.')
        } finally {
            setProcessingId(null)
        }
    }

    return (
        <section className="min-w-0 border-t-2 border-ink pt-5">
            <h2 className="mb-2 flex items-center gap-2 text-xl font-bold">
                <CircleDollarSign />
                월별 정산 관리
            </h2>
            <p className="mb-6 text-sm text-muted">
                지급 완료는 포트폴리오용 모의 처리이며 실제 계좌 이체를 실행하지 않습니다.
            </p>
            {message && <p className="mb-4 border border-success/35 bg-success-soft p-3 text-sm text-success">{message}</p>}
            {errorMessage && <p className="mb-4 border border-danger/35 bg-danger-soft p-3 text-sm text-danger" role="alert">{errorMessage}</p>}
            {isLoading ? (
                <div className="grid min-h-28 place-content-center">
                    <LoaderCircle className="size-5 animate-spin" />
                </div>
            ) : requests.length === 0 ? (
                <p className="text-sm text-muted">정산 신청이 없습니다.</p>
            ) : (
                <div className="grid gap-3">
                    {requests.map((request) => (
                        <article className="border border-line p-4" key={request.settlementRequestId}>
                            <div className="flex flex-wrap items-start justify-between gap-3">
                                <div>
                                    <strong>{request.storeName} · 정산 #{request.settlementRequestId}</strong>
                                    <p className="mt-1 text-xs text-muted">
                                        신청 {formatKoreanDateTime(request.createdAt)}
                                    </p>
                                </div>
                                <StatusBadge tone={settlementStatusTone[request.status]}>
                                    {statusLabel[request.status]}
                                </StatusBadge>
                            </div>
                            <div className="mt-4 grid gap-2 text-sm min-[701px]:grid-cols-3">
                                <p>매출 <b>{formatPrice(request.grossAmount)}</b></p>
                                <p>수수료 <b>{formatPrice(request.feeAmount)}</b></p>
                                <p>지급 예정 <b>{formatPrice(request.settlementAmount)}</b></p>
                            </div>
                            {request.rejectionReason && (
                                <p className="mt-3 text-sm text-danger">
                                    반려 사유: {request.rejectionReason}
                                </p>
                            )}
                            {request.status === 'REQUESTED' && (
                                <div className="mt-4 grid gap-3">
                                    {rejectingId === request.settlementRequestId && (
                                        <textarea
                                            className="min-h-20 border border-line bg-surface p-3 text-sm text-ink"
                                            maxLength={500}
                                            placeholder="반려 사유를 입력하세요."
                                            value={rejectionReason}
                                            onChange={(event) => setRejectionReason(event.target.value)}
                                        />
                                    )}
                                    <div className="flex flex-wrap gap-2">
                                        <button
                                            className="flex h-10 items-center gap-1.5 bg-ink px-4 text-xs font-bold text-surface disabled:opacity-50"
                                            type="button"
                                            disabled={processingId !== null}
                                            onClick={() => void process(
                                                request.settlementRequestId,
                                                () => approveAdminSettlementRequest(request.settlementRequestId),
                                                '정산 신청을 승인했습니다.',
                                            )}
                                        >
                                            <Check className="size-4" /> 승인
                                        </button>
                                        <button
                                            className="flex h-10 items-center gap-1.5 border border-danger px-4 text-xs font-bold text-danger disabled:opacity-50"
                                            type="button"
                                            disabled={processingId !== null}
                                            onClick={() => {
                                                if (rejectingId !== request.settlementRequestId) {
                                                    setRejectingId(request.settlementRequestId)
                                                    setRejectionReason('')
                                                    setErrorMessage('')
                                                    return
                                                }
                                                if (!rejectionReason.trim()) {
                                                    setErrorMessage('반려 사유를 입력해 주세요.')
                                                    return
                                                }
                                                void process(
                                                    request.settlementRequestId,
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
                                </div>
                            )}
                            {request.status === 'APPROVED' && (
                                <button
                                    className="mt-4 h-10 bg-accent px-4 text-xs font-bold text-paper disabled:opacity-50"
                                    type="button"
                                    disabled={processingId !== null}
                                    onClick={() => void process(
                                        request.settlementRequestId,
                                        () => completeAdminMockSettlementPayment(request.settlementRequestId),
                                        '모의 지급 처리를 완료했습니다.',
                                    )}
                                >
                                    모의 지급 완료 처리
                                </button>
                            )}
                        </article>
                    ))}
                </div>
            )}
        </section>
    )
}
