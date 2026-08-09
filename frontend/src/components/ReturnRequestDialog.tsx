import { useState, type FormEvent } from 'react'
import type { OrderItem, ReturnRequestCreateRequest } from '../types/order'

interface ReturnRequestDialogProps {
    open: boolean
    item: OrderItem | null
    isSubmitting: boolean
    errorMessage: string
    onClose: () => void
    onSubmit: (request: ReturnRequestCreateRequest) => Promise<boolean>
}

export function ReturnRequestDialog({
    open,
    item,
    isSubmitting,
    errorMessage,
    onClose,
    onSubmit,
}: ReturnRequestDialogProps) {
    const [quantity, setQuantity] = useState(1)
    const [reason, setReason] = useState('')
    const maxQuantity = item ? item.quantity - item.refundedQuantity : 0

    if (!open || !item) return null

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        if (!item || !reason.trim()) return
        const completed = await onSubmit({
            orderItemId: item.orderItemId,
            quantity,
            reason: reason.trim(),
        })
        if (completed) onClose()
    }

    return (
        <div
            className="fixed inset-0 z-100 grid place-items-center bg-black/45 px-5"
            role="presentation"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && !isSubmitting) onClose()
            }}
        >
            <section
                className="w-full max-w-125 border border-ink bg-paper p-6 shadow-2xl min-[601px]:p-8"
                role="dialog"
                aria-modal="true"
                aria-labelledby="return-request-title"
            >
                <p className="text-[11px] font-extrabold tracking-[.18em] text-accent">
                    RETURN REQUEST
                </p>
                <h2 className="mt-3 font-serif text-3xl" id="return-request-title">
                    반품 신청
                </h2>
                <p className="mt-3 text-sm font-bold">{item.productName}</p>
                <p className="mt-1 text-xs text-muted">
                    배송 완료 후 7일 이내에 신청할 수 있으며, 판매자 승인 시 환불과 재고 복구가 처리됩니다.
                </p>

                <form className="mt-6 grid gap-5" onSubmit={(event) => void handleSubmit(event)}>
                    <label className="grid gap-2 text-xs font-bold">
                        반품 수량
                        <input
                            className="h-11 border border-line bg-surface px-3 text-sm"
                            type="number"
                            min={1}
                            max={maxQuantity}
                            value={quantity}
                            disabled={isSubmitting}
                            onChange={(event) => setQuantity(Number(event.target.value))}
                        />
                    </label>
                    <label className="grid gap-2 text-xs font-bold">
                        반품 사유
                        <textarea
                            className="min-h-28 resize-y border border-line bg-surface p-3 text-sm"
                            maxLength={500}
                            required
                            value={reason}
                            disabled={isSubmitting}
                            placeholder="상품 상태와 반품 사유를 구체적으로 입력해 주세요."
                            onChange={(event) => setReason(event.target.value)}
                        />
                        <span className="text-right text-[11px] font-normal text-muted">
                            {reason.length}/500
                        </span>
                    </label>
                    {errorMessage && (
                        <p className="text-xs font-bold text-danger">
                            {errorMessage}
                        </p>
                    )}
                    <div className="grid grid-cols-2 gap-3">
                        <button
                            className="h-11 border border-ink bg-surface text-xs font-bold"
                            type="button"
                            disabled={isSubmitting}
                            onClick={onClose}
                        >
                            취소
                        </button>
                        <button
                            className="h-11 bg-ink text-xs font-bold text-white disabled:opacity-50"
                            type="submit"
                            disabled={
                                isSubmitting
                                || !reason.trim()
                                || quantity < 1
                                || quantity > maxQuantity
                            }
                        >
                            {isSubmitting ? '신청 중...' : '반품 신청'}
                        </button>
                    </div>
                </form>
            </section>
        </div>
    )
}
