import { LoaderCircle } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { cancelOrder, getOrder, processMockPayment } from '../api/orders'
import type { Order, PaymentResult } from '../types/order'
import { getOrderStatusLabel } from '../utils/order'
import { formatPrice } from '../utils/product'

export function PaymentPage() {
    const { orderId: orderIdParam } = useParams()
    const orderId = Number(orderIdParam)
    const isValidOrderId = Number.isInteger(orderId) && orderId > 0
    const [order, setOrder] = useState<Order | null>(null)
    const [isLoading, setIsLoading] = useState(isValidOrderId)
    const [pendingAction, setPendingAction] = useState<PaymentResult | 'CANCEL' | null>(null)
    const [errorMessage, setErrorMessage] = useState('')
    const successKeyRef = useRef(crypto.randomUUID())
    const failureKeyRef = useRef(crypto.randomUUID())
    const navigate = useNavigate()

    useEffect(() => {
        if (!isValidOrderId) return
        const controller = new AbortController()
        getOrder(orderId, controller.signal)
            .then(setOrder)
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError ? error.message : '주문을 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [isValidOrderId, orderId])

    async function pay(result: PaymentResult) {
        if (!order || pendingAction) return
        setPendingAction(result)
        setErrorMessage('')
        try {
            const idempotencyKey = result === 'SUCCESS'
                ? successKeyRef.current
                : failureKeyRef.current
            await processMockPayment(order.orderId, { idempotencyKey, result })
            navigate(`/orders/${order.orderId}/result`)
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '결제를 처리하지 못했습니다.')
        } finally {
            setPendingAction(null)
        }
    }

    async function cancel() {
        if (!order || pendingAction) return
        setPendingAction('CANCEL')
        setErrorMessage('')
        try {
            await cancelOrder(order.orderId)
            navigate(`/orders/${order.orderId}/result`)
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '주문을 취소하지 못했습니다.')
        } finally {
            setPendingAction(null)
        }
    }

    if (isLoading) {
        return <div className="grid min-h-100 place-content-center text-sm text-muted">주문을 확인하고 있습니다.</div>
    }

    if (!isValidOrderId) {
        return <div className="grid min-h-100 place-content-center gap-4 text-center"><p>올바르지 않은 주문 번호입니다.</p><Link className="text-xs underline" to="/orders">주문 내역으로</Link></div>
    }

    if (!order) {
        return <div className="grid min-h-100 place-content-center gap-4 text-center"><p>{errorMessage}</p><Link className="text-xs underline" to="/orders">주문 내역으로</Link></div>
    }

    const canPay = order.status === 'PENDING_PAYMENT' || order.status === 'PAYMENT_FAILED'

    return (
        <section className="mx-auto max-w-180 px-4 py-16 min-[601px]:py-24">
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">MOCK PAYMENT</p>
            <h1 className="font-serif text-[clamp(42px,7vw,68px)] leading-none tracking-tighter">결제하기</h1>
            <div className="my-10 border-y border-ink py-6">
                <div className="flex justify-between text-sm"><span>주문 번호</span><b>#{order.orderId}</b></div>
                <div className="mt-3 flex justify-between text-sm"><span>주문 상태</span><b>{getOrderStatusLabel(order.status)}</b></div>
                <div className="mt-5 flex items-baseline justify-between border-t border-line pt-5"><span className="text-sm font-bold">결제 금액</span><strong className="text-2xl">{formatPrice(order.totalAmount)}</strong></div>
            </div>

            {errorMessage && <p className="mb-5 text-sm text-[#b23b2f]" role="alert">{errorMessage}</p>}

            {canPay ? (
                <div className="grid gap-3 min-[601px]:grid-cols-2">
                    <button className="h-13 border-0 bg-ink font-bold text-white disabled:opacity-50" type="button" disabled={pendingAction !== null} onClick={() => pay('SUCCESS')}>
                        {pendingAction === 'SUCCESS' ? <LoaderCircle className="mx-auto size-5 animate-spin" /> : '결제 성공 처리'}
                    </button>
                    <button className="h-13 border border-[#b23b2f] bg-white font-bold text-[#b23b2f] disabled:opacity-50" type="button" disabled={pendingAction !== null} onClick={() => pay('FAILURE')}>
                        {pendingAction === 'FAILURE' ? <LoaderCircle className="mx-auto size-5 animate-spin" /> : '결제 실패 시뮬레이션'}
                    </button>
                    <button className="h-11 border-0 bg-transparent text-xs underline disabled:opacity-50 min-[601px]:col-span-2" type="button" disabled={pendingAction !== null} onClick={cancel}>
                        {pendingAction === 'CANCEL' ? '취소 처리 중...' : '주문 취소 및 재고 복구'}
                    </button>
                </div>
            ) : (
                <Link className="grid h-13 place-items-center bg-ink text-sm font-bold text-white" to={`/orders/${order.orderId}/result`}>결과 확인</Link>
            )}
        </section>
    )
}
