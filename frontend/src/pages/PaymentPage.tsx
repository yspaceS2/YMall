import { loadTossPayments } from '@tosspayments/tosspayments-sdk'
import { LoaderCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { cancelOrder, getOrder } from '../api/orders'
import { getAccessToken, getTokenSubject } from '../auth/tokenStorage'
import type { Order } from '../types/order'
import { getOrderStatusLabel } from '../utils/order'
import { formatPrice } from '../utils/product'
import { getTossCustomerKey, getTossErrorMessage } from '../utils/tossPayments'

const tossClientKey = (import.meta.env.VITE_TOSS_CLIENT_KEY ?? '').trim()

export function PaymentPage() {
    const { orderId: orderIdParam } = useParams()
    const orderId = Number(orderIdParam)
    const isValidOrderId = Number.isInteger(orderId) && orderId > 0
    const [order, setOrder] = useState<Order | null>(null)
    const [isLoading, setIsLoading] = useState(isValidOrderId)
    const [pendingAction, setPendingAction] = useState<'PAYMENT' | 'CANCEL' | null>(null)
    const [errorMessage, setErrorMessage] = useState('')
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

    async function openPaymentWindow() {
        if (!order || pendingAction) return
        if (!tossClientKey) {
            setErrorMessage('Toss Payments 클라이언트 키가 설정되지 않았습니다.')
            return
        }

        setPendingAction('PAYMENT')
        setErrorMessage('')
        try {
            const tossPayments = await loadTossPayments(tossClientKey)
            const memberKey = getTokenSubject(getAccessToken())
            if (!memberKey) {
                throw new Error('로그인 정보를 확인할 수 없습니다. 다시 로그인해 주세요.')
            }
            const payment = tossPayments.payment({ customerKey: getTossCustomerKey(memberKey) })
            const origin = window.location.origin
            const itemCount = order.items.length
            const firstItemName = order.items[0]?.productName ?? 'YMall 상품'
            const orderName = (itemCount > 1 ? `${firstItemName} 외 ${itemCount - 1}건` : firstItemName)
                .slice(0, 100)

            await payment.requestPayment({
                method: 'CARD',
                amount: {
                    currency: 'KRW',
                    value: order.totalAmount,
                },
                orderId: order.paymentOrderId,
                orderName,
                customerName: order.deliveryAddress?.recipientName,
                customerMobilePhone: order.deliveryAddress?.recipientPhone.replace(/\D/g, ''),
                successUrl: `${origin}/orders/${order.orderId}/payment/success`,
                failUrl: `${origin}/orders/${order.orderId}/payment/fail`,
                windowTarget: 'self',
            })
            setPendingAction(null)
        } catch (error) {
            setErrorMessage(getTossErrorMessage(error))
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
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-accent">TOSS PAYMENTS</p>
            <h1 className="font-serif text-[clamp(42px,7vw,68px)] leading-none tracking-tighter">결제하기</h1>
            <div className="my-10 border-y border-ink py-6">
                <div className="flex justify-between text-sm"><span>주문 번호</span><b>#{order.orderId}</b></div>
                <div className="mt-3 flex justify-between text-sm"><span>주문 상태</span><b>{getOrderStatusLabel(order.status)}</b></div>
                <div className="mt-5 flex items-baseline justify-between border-t border-line pt-5"><span className="text-sm font-bold">결제 금액</span><strong className="text-2xl">{formatPrice(order.totalAmount)}</strong></div>
            </div>

            {errorMessage && <p className="mb-5 text-sm text-danger" role="alert">{errorMessage}</p>}

            {canPay ? (
                <div className="grid gap-3">
                    <button
                        className="h-13 border-0 bg-ink font-bold text-white disabled:opacity-50"
                        type="button"
                        disabled={pendingAction !== null}
                        onClick={openPaymentWindow}
                    >
                        {pendingAction === 'PAYMENT' ? <LoaderCircle className="mx-auto size-5 animate-spin" /> : `${formatPrice(order.totalAmount)} 결제하기`}
                    </button>
                    <p className="text-center text-[11px] leading-5 text-muted">결제 버튼을 누르면 Toss Payments의 안전한 결제창으로 이동합니다.</p>
                    <button className="h-11 border-0 bg-transparent text-xs underline disabled:opacity-50" type="button" disabled={pendingAction !== null} onClick={cancel}>
                        {pendingAction === 'CANCEL' ? '취소 처리 중...' : '주문 취소 및 재고 복구'}
                    </button>
                </div>
            ) : (
                <Link className="grid h-13 place-items-center bg-ink text-sm font-bold text-white" to={`/orders/${order.orderId}/result`}>결과 확인</Link>
            )}
        </section>
    )
}
