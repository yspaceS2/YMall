import { LoaderCircle, RotateCcw, X } from 'lucide-react'
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import type {
    OrderItemFulfillmentStatus,
    PaymentRefund,
    PaymentRefundRequest,
} from '../types/order'
import { formatPrice } from '../utils/product'

export interface RefundableOrderItem {
    orderItemId: number
    productName: string
    unitPrice: number
    quantity: number
    refundedQuantity: number
    fulfillmentStatus: OrderItemFulfillmentStatus
}

interface RefundDialogProps {
    open: boolean
    orderId: number | null
    items: RefundableOrderItem[]
    refunds: PaymentRefund[]
    isLoadingHistory: boolean
    isSubmitting: boolean
    errorMessage: string
    mode?: 'refund' | 'sellerCancel'
    onClose: () => void
    onSubmit: (request: PaymentRefundRequest) => Promise<boolean>
}

export function RefundDialog({
    open,
    orderId,
    items,
    refunds,
    isLoadingHistory,
    isSubmitting,
    errorMessage,
    mode = 'refund',
    onClose,
    onSubmit,
}: RefundDialogProps) {
    const [reason, setReason] = useState('')
    const [quantities, setQuantities] = useState<Record<number, number>>({})
    const [idempotencyKey, setIdempotencyKey] = useState(() =>
        orderId === null ? '' : `refund-${orderId}-${crypto.randomUUID()}`
    )

    useEffect(() => {
        if (!open) return
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape' && !isSubmitting) onClose()
        }
        window.addEventListener('keydown', handleKeyDown)
        return () => window.removeEventListener('keydown', handleKeyDown)
    }, [isSubmitting, onClose, open])

    const refundableItems = useMemo(
        () => items.filter((item) =>
            item.fulfillmentStatus === 'PENDING'
            && item.quantity > item.refundedQuantity
        ),
        [items],
    )
    const selectedItems = refundableItems
        .map((item) => ({
            orderItemId: item.orderItemId,
            quantity: quantities[item.orderItemId] ?? 0,
        }))
        .filter((item) => item.quantity > 0)
    const selectedAmount = refundableItems.reduce(
        (total, item) => total + item.unitPrice * (quantities[item.orderItemId] ?? 0),
        0,
    )

    if (!open || orderId === null) return null
    const isSellerCancel = mode === 'sellerCancel'

    function updateQuantity(item: RefundableOrderItem, quantity: number) {
        const remaining = item.quantity - item.refundedQuantity
        setQuantities((current) => ({
            ...current,
            [item.orderItemId]: Math.min(Math.max(quantity, 0), remaining),
        }))
    }

    function selectAllRemaining() {
        setQuantities(Object.fromEntries(
            refundableItems.map((item) => [
                item.orderItemId,
                item.quantity - item.refundedQuantity,
            ]),
        ))
    }

    async function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        if (!reason.trim() || selectedItems.length === 0 || !idempotencyKey) return
        const succeeded = await onSubmit({
            idempotencyKey,
            reason: reason.trim(),
            items: selectedItems,
        })
        if (succeeded) {
            setReason('')
            setQuantities({})
            setIdempotencyKey(`refund-${orderId}-${crypto.randomUUID()}`)
        }
    }

    return (
        <div
            className="fixed inset-0 z-50 grid place-items-center bg-black/45 p-4"
            role="presentation"
            onMouseDown={(event) => {
                if (event.currentTarget === event.target && !isSubmitting) onClose()
            }}
        >
            <section
                className="max-h-[90vh] w-full max-w-170 overflow-y-auto bg-[#fafaf7] p-5 shadow-2xl min-[601px]:p-8"
                role="dialog"
                aria-modal="true"
                aria-labelledby="refund-dialog-title"
            >
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <p className="text-[10px] font-extrabold tracking-[.18em] text-[#71801e]">
                            ORDER #{orderId}
                        </p>
                        <h2 id="refund-dialog-title" className="mt-2 text-2xl font-bold">
                            {isSellerCancel ? '판매 취소 및 환불 처리' : '환불 신청 및 내역'}
                        </h2>
                    </div>
                    <button
                        className="grid size-9 place-items-center border border-line bg-white"
                        type="button"
                        disabled={isSubmitting}
                        onClick={onClose}
                        aria-label="환불 창 닫기"
                    >
                        <X className="size-4" />
                    </button>
                </div>

                <form className="mt-7" onSubmit={submit}>
                    <div className="flex items-center justify-between gap-3">
                        <h3 className="text-sm font-bold">
                            {isSellerCancel ? '판매 취소할 상품' : '환불할 상품'}
                        </h3>
                        {refundableItems.length > 0 && (
                            <button
                                className="text-xs font-bold underline"
                                type="button"
                                onClick={selectAllRemaining}
                            >
                                남은 수량 전체 선택
                            </button>
                        )}
                    </div>
                    <div className="mt-3 divide-y divide-line border-y border-line">
                        {refundableItems.length === 0 ? (
                            <p className="py-5 text-sm text-muted">
                                현재 환불 가능한 상품이 없습니다.
                            </p>
                        ) : refundableItems.map((item) => {
                            const remaining = item.quantity - item.refundedQuantity
                            return (
                                <label
                                    className="flex items-center justify-between gap-4 py-4"
                                    key={item.orderItemId}
                                >
                                    <span>
                                        <strong className="block text-sm">{item.productName}</strong>
                                        <span className="mt-1 block text-xs text-muted">
                                            환불 가능 {remaining}개 · {formatPrice(item.unitPrice)}
                                        </span>
                                    </span>
                                    <input
                                        className="h-10 w-20 border border-line bg-white px-3 text-right"
                                        type="number"
                                        min={0}
                                        max={remaining}
                                        value={quantities[item.orderItemId] ?? 0}
                                        onChange={(event) =>
                                            updateQuantity(item, Number(event.target.value))
                                        }
                                    />
                                </label>
                            )
                        })}
                    </div>

                    <label className="mt-5 grid gap-2 text-xs font-bold">
                        {isSellerCancel ? '판매 취소 사유' : '환불 사유'}
                        <textarea
                            className="min-h-24 resize-y border border-line bg-white p-3 text-sm font-normal outline-none focus:border-ink"
                            value={reason}
                            maxLength={200}
                            placeholder={
                                isSellerCancel
                                    ? '재고 착오 등 판매 취소 사유를 입력해 주세요.'
                                    : '환불 사유를 입력해 주세요.'
                            }
                            onChange={(event) => setReason(event.target.value)}
                        />
                    </label>
                    <div className="mt-4 flex items-center justify-between gap-4">
                        <span className="text-sm">
                            선택 금액 <b>{formatPrice(selectedAmount)}</b>
                        </span>
                        <button
                            className="h-11 bg-ink px-6 text-xs font-bold text-white disabled:opacity-50"
                            type="submit"
                            disabled={
                                isSubmitting
                                || selectedItems.length === 0
                                || !reason.trim()
                            }
                        >
                            {isSubmitting
                                ? '환불 처리 중...'
                                : isSellerCancel
                                    ? '판매 취소 및 환불'
                                    : '환불 신청'}
                        </button>
                    </div>
                    {errorMessage && (
                        <p className="mt-4 border border-[#e2b9b4] bg-[#fff5f3] p-3 text-sm text-[#a22e24]">
                            {errorMessage}
                        </p>
                    )}
                </form>

                <div className="mt-9 border-t-2 border-ink pt-5">
                    <h3 className="flex items-center gap-2 text-sm font-bold">
                        <RotateCcw className="size-4" /> 환불 처리 내역
                    </h3>
                    {isLoadingHistory ? (
                        <LoaderCircle className="mx-auto my-7 size-5 animate-spin" />
                    ) : refunds.length === 0 ? (
                        <p className="py-5 text-sm text-muted">아직 환불 내역이 없습니다.</p>
                    ) : (
                        <div className="mt-3 grid gap-3">
                            {refunds.map((refund) => (
                                <article className="border border-line bg-white p-4" key={refund.refundId}>
                                    <div className="flex flex-wrap items-center justify-between gap-2">
                                        <strong className="text-sm">
                                            {refund.type === 'FULL' ? '전체 환불' : '부분 환불'}
                                            {' · '}
                                            {formatPrice(refund.amount)}
                                        </strong>
                                        <RefundStatusBadge status={refund.status} />
                                    </div>
                                    <p className="mt-2 text-xs text-muted">{refund.reason}</p>
                                    <p className="mt-2 text-xs">
                                        {refund.items
                                            .map((item) => `${item.productName} ${item.quantity}개`)
                                            .join(', ')}
                                    </p>
                                    {refund.failureMessage && (
                                        <p className="mt-3 text-xs text-[#a22e24]">
                                            {refund.failureMessage}
                                        </p>
                                    )}
                                </article>
                            ))}
                        </div>
                    )}
                </div>
            </section>
        </div>
    )
}

function RefundStatusBadge({ status }: { status: PaymentRefund['status'] }) {
    const labels = {
        PENDING: '처리 중',
        SUCCEEDED: '환불 완료',
        FAILED: '처리 실패',
        UNKNOWN: '결과 확인 중',
    }
    const colors = status === 'SUCCEEDED'
        ? 'bg-[#eef0df] text-[#66751c]'
        : status === 'FAILED'
            ? 'bg-[#fff0ed] text-[#a22e24]'
            : 'bg-[#f2efe6] text-[#6b6044]'
    return (
        <span className={`px-2.5 py-1 text-[10px] font-extrabold ${colors}`}>
            {labels[status]}
        </span>
    )
}
