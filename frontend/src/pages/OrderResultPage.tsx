import { CircleCheck, CircleX, Clock3 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { getOrder } from '../api/orders'
import type { Order } from '../types/order'
import { getOrderStatusLabel } from '../utils/order'
import { formatPrice } from '../utils/product'

export function OrderResultPage() {
    const { orderId: orderIdParam } = useParams()
    const orderId = Number(orderIdParam)
    const isValidOrderId = Number.isInteger(orderId) && orderId > 0
    const [order, setOrder] = useState<Order | null>(null)
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        if (!isValidOrderId) return
        const controller = new AbortController()
        getOrder(orderId, controller.signal)
            .then(setOrder)
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError ? error.message : '주문 결과를 불러오지 못했습니다.')
            })
        return () => controller.abort()
    }, [isValidOrderId, orderId])

    if (!isValidOrderId) {
        return <div className="grid min-h-100 place-content-center gap-4 text-center text-sm"><p>올바르지 않은 주문 번호입니다.</p><Link className="text-xs underline" to="/orders">주문 내역으로</Link></div>
    }

    if (!order) {
        return <div className="grid min-h-100 place-content-center gap-4 text-center text-sm"><p>{errorMessage || '주문 결과를 확인하고 있습니다.'}</p>{errorMessage && <Link className="text-xs underline" to="/orders">주문 내역으로</Link>}</div>
    }

    const isSuccess = order.status === 'PAID'
    const isFailed = order.status === 'PAYMENT_FAILED'
    const Icon = isSuccess ? CircleCheck : isFailed || order.status === 'CANCELED' ? CircleX : Clock3

    return (
        <section className="mx-auto grid min-h-130 max-w-180 place-content-center justify-items-center px-4 py-16 text-center">
            <Icon className={`mb-6 size-12 ${isSuccess ? 'text-[#71801e]' : isFailed ? 'text-[#b23b2f]' : 'text-muted'}`} />
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">ORDER RESULT</p>
            <h1 className="font-serif text-[clamp(42px,7vw,68px)] leading-none tracking-tighter">{getOrderStatusLabel(order.status)}</h1>
            <p className="mt-6 text-sm text-muted">주문 #{order.orderId} · {formatPrice(order.totalAmount)}</p>
            {isFailed && <p className="mt-3 text-sm text-[#b23b2f]">결제에 실패했습니다. 주문을 유지한 채 다시 시도할 수 있습니다.</p>}
            <div className="mt-8 flex flex-wrap justify-center gap-3">
                {isFailed && <Link className="bg-ink px-6 py-3 text-xs font-bold text-white" to={`/orders/${order.orderId}/payment`}>결제 다시 시도</Link>}
                <Link className="border border-ink px-6 py-3 text-xs font-bold" to="/orders">주문 내역 보기</Link>
                <Link className="px-6 py-3 text-xs underline" to="/">쇼핑 계속하기</Link>
            </div>
        </section>
    )
}
