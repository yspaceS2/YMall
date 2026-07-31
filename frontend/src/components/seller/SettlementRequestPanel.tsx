import { LoaderCircle, WalletCards } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'

import { ApiError } from '../../api/client'
import {
    createSettlementRequest,
    getSettlementAvailability,
} from '../../api/seller'
import type { SettlementAvailability } from '../../types/seller'
import { formatPrice } from '../../utils/product'
import { SellerSettlementRequestList } from '../settlement/SettlementRequestList'
import { FeedbackMessage } from '../ui/FeedbackMessage'

export function SettlementRequestPanel({
    view,
}: {
    view: 'request' | 'history'
}) {
    const [availability, setAvailability] = useState<SettlementAvailability | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [successMessage, setSuccessMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')

    const load = useCallback(async (signal?: AbortSignal) => {
        setAvailability(await getSettlementAvailability(signal))
    }, [])

    useEffect(() => {
        const controller = new AbortController()
        getSettlementAvailability(controller.signal)
            .then(setAvailability)
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
    }, [])

    async function submit() {
        setIsSubmitting(true)
        setSuccessMessage('')
        setErrorMessage('')
        try {
            await createSettlementRequest()
            await load()
            setSuccessMessage('정산을 신청했습니다.')
        } catch (error) {
            setErrorMessage(error instanceof ApiError
                ? error.message
                : '정산을 신청하지 못했습니다.')
        } finally {
            setIsSubmitting(false)
        }
    }

    if (view === 'history') {
        return <SellerSettlementRequestList />
    }

    return (
        <section className="border-t-2 border-ink pt-5" aria-labelledby="settlement-request-title">
            <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
                <div>
                    <h2 className="flex items-center gap-2 text-xl font-bold" id="settlement-request-title">
                        <WalletCards className="size-5" aria-hidden="true" />
                        정산 신청
                    </h2>
                    <p className="mt-2 max-w-160 text-sm leading-6 text-muted">
                        정산 조건을 충족한 금액은 월 마감 없이 신청할 수 있습니다.
                        신청하면 현재 정산 가능 금액 전액이 처리 중 상태로 전환됩니다.
                    </p>
                </div>
            </div>

            {successMessage && (
                <FeedbackMessage className="mb-4" tone="success">
                    {successMessage}
                </FeedbackMessage>
            )}
            {errorMessage && (
                <FeedbackMessage className="mb-4" tone="error">
                    {errorMessage}
                </FeedbackMessage>
            )}

            {isLoading ? (
                <div className="grid min-h-28 place-content-center">
                    <LoaderCircle className="size-5 animate-spin" />
                </div>
            ) : availability && (
                <div className="grid gap-4">
                    <div className="grid gap-px border-y-2 border-ink bg-line min-[701px]:grid-cols-2 min-[1101px]:grid-cols-4">
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
                            ? `${formatPrice(availability.settlementAmount)} 정산 신청`
                            : availability.hasSettlementAccount
                                ? '신청 가능한 금액이 없습니다'
                                : '정산 계좌를 먼저 등록해 주세요'}
                    </button>
                </div>
            )}
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
        <div className="flex items-center justify-between gap-4 bg-surface px-4 py-4">
            <p className="text-xs font-bold text-muted">{label}</p>
            <strong className={strong ? 'text-lg' : 'text-sm'}>{value}</strong>
        </div>
    )
}
