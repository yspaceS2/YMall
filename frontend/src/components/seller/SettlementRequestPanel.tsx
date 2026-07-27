import { CalendarRange, LoaderCircle, WalletCards } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'

import { ApiError } from '../../api/client'
import {
    createSettlementRequest,
    getSettlementAvailability,
    getSettlementRequests,
} from '../../api/seller'
import type {
    SettlementAvailability,
    SettlementRequest,
    SettlementRequestStatus,
} from '../../types/seller'
import { formatKoreanDateTime } from '../../utils/dateTime'
import { formatPrice } from '../../utils/product'

const statusLabel: Record<SettlementRequestStatus, string> = {
    REQUESTED: '신청 완료',
    APPROVED: '승인 완료',
    REJECTED: '반려',
    PAID: '모의 지급 완료',
}

export function SettlementRequestPanel() {
    const [period, setPeriod] = useState(previousMonth())
    const [availability, setAvailability] = useState<SettlementAvailability | null>(null)
    const [requests, setRequests] = useState<SettlementRequest[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [message, setMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')

    const load = useCallback(async (signal?: AbortSignal) => {
        const [availabilityResponse, requestPage] = await Promise.all([
            getSettlementAvailability(period, signal),
            getSettlementRequests(signal),
        ])
        setAvailability(availabilityResponse)
        setRequests(requestPage.content)
    }, [period])

    useEffect(() => {
        const controller = new AbortController()
        Promise.all([
            getSettlementAvailability(period, controller.signal),
            getSettlementRequests(controller.signal),
        ])
            .then(([availabilityResponse, requestPage]) => {
                setAvailability(availabilityResponse)
                setRequests(requestPage.content)
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError
                    ? error.message
                    : '정산 정보를 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [period])

    async function submit() {
        setIsSubmitting(true)
        setMessage('')
        setErrorMessage('')
        try {
            await createSettlementRequest(period)
            await load()
            setMessage('월별 정산을 신청했습니다.')
        } catch (error) {
            setErrorMessage(error instanceof ApiError
                ? error.message
                : '정산을 신청하지 못했습니다.')
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <section className="border-t-2 border-ink pt-5">
            <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
                <div>
                    <h2 className="flex items-center gap-2 text-xl font-bold">
                        <WalletCards className="size-5" />
                        월별 정산
                    </h2>
                    <p className="mt-2 text-sm text-muted">
                        종료된 월의 배송 완료 매출을 정산 신청할 수 있습니다.
                    </p>
                </div>
                <label className="grid gap-1 text-xs font-bold">
                    정산 대상 월
                    <input
                        className="h-10 border border-line bg-surface px-3 text-sm text-ink"
                        type="month"
                        max={previousMonth()}
                        value={period}
                        onChange={(event) => {
                            setIsLoading(true)
                            setErrorMessage('')
                            setPeriod(event.target.value)
                        }}
                    />
                </label>
            </div>

            {message && <p className="mb-4 border border-[#cad39b] bg-[#f4f6e8] p-3 text-sm text-[#46510f] dark:border-[#59652a] dark:bg-[#283010] dark:text-[#dce9a6]">{message}</p>}
            {errorMessage && <p className="mb-4 border border-[#e2b9b4] bg-[#fff5f3] p-3 text-sm text-[#a22e24] dark:border-[#7d4039] dark:bg-[#351915] dark:text-[#ffb7ae]" role="alert">{errorMessage}</p>}

            {isLoading ? (
                <div className="grid min-h-28 place-content-center">
                    <LoaderCircle className="size-5 animate-spin" />
                </div>
            ) : availability && (
                <div className="grid gap-4">
                    <div className="grid gap-3 min-[701px]:grid-cols-4">
                        <Metric label="원장 건수" value={`${availability.entryCount}건`} />
                        <Metric label="매출" value={formatPrice(availability.grossAmount)} />
                        <Metric label="수수료" value={formatPrice(availability.feeAmount)} />
                        <Metric label="정산 예정액" value={formatPrice(availability.settlementAmount)} strong />
                    </div>
                    <button
                        className="h-11 bg-ink px-5 text-sm font-bold text-surface disabled:cursor-not-allowed disabled:opacity-40"
                        type="button"
                        disabled={!availability.canRequest || isSubmitting}
                        onClick={() => void submit()}
                    >
                        {isSubmitting ? '신청 중...' : availability.canRequest
                            ? `${period} 정산 신청`
                            : availability.hasSettlementAccount
                                ? '신청 가능한 금액이 없습니다'
                                : '정산 계좌를 먼저 등록해 주세요'}
                    </button>
                </div>
            )}

            <div className="mt-8">
                <h3 className="mb-3 flex items-center gap-2 font-bold">
                    <CalendarRange className="size-4" />
                    신청 이력
                </h3>
                {requests.length === 0 ? (
                    <p className="text-sm text-muted">정산 신청 이력이 없습니다.</p>
                ) : (
                    <div className="grid gap-3">
                        {requests.map((request) => (
                            <article className="border border-line p-4" key={request.settlementRequestId}>
                                <div className="flex flex-wrap items-center justify-between gap-2">
                                    <strong>{request.periodStart.slice(0, 7)}</strong>
                                    <span className="border border-line px-2.5 py-1 text-xs font-bold">
                                        {statusLabel[request.status]}
                                    </span>
                                </div>
                                <p className="mt-2 text-sm">
                                    정산액 <b>{formatPrice(request.settlementAmount)}</b>
                                </p>
                                <p className="mt-1 text-xs text-muted">
                                    신청 {formatKoreanDateTime(request.createdAt)}
                                </p>
                                {request.rejectionReason && (
                                    <p className="mt-2 text-sm text-[#a22e24] dark:text-[#ffb7ae]">
                                        반려 사유: {request.rejectionReason}
                                    </p>
                                )}
                                {request.status === 'PAID' && (
                                    <p className="mt-2 text-xs font-bold text-[#71801e] dark:text-[#c9db72]">
                                        포트폴리오용 모의 지급이며 실제 계좌 이체는 발생하지 않았습니다.
                                    </p>
                                )}
                            </article>
                        ))}
                    </div>
                )}
            </div>
        </section>
    )
}

function Metric({
    label,
    value,
    strong = false,
}: {
    label: string
    value: string
    strong?: boolean
}) {
    return (
        <div className="border border-line p-4">
            <p className="text-xs text-muted">{label}</p>
            <strong className={`mt-2 block ${strong ? 'text-xl' : 'text-base'}`}>{value}</strong>
        </div>
    )
}

function previousMonth() {
    const date = new Date()
    date.setDate(1)
    date.setMonth(date.getMonth() - 1)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}
