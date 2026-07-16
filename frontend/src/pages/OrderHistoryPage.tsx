import { ReceiptText } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/client'
import { getOrders } from '../api/orders'
import type { Order } from '../types/order'
import { formatOrderDate, getOrderStatusLabel } from '../utils/order'
import { formatPrice } from '../utils/product'

export function OrderHistoryPage() {
    const [orders, setOrders] = useState<Order[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        getOrders(1, 20, controller.signal)
            .then((page) => setOrders(page.content))
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError ? error.message : '주문 내역을 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [])

    if (isLoading) {
        return <div className="grid min-h-100 place-content-center text-sm text-muted">주문 내역을 불러오고 있습니다.</div>
    }

    return (
        <section className="mx-auto max-w-260 px-4 py-14 min-[601px]:px-8 min-[601px]:py-20">
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">MY ORDERS</p>
            <h1 className="mb-10 font-serif text-[clamp(42px,6vw,68px)] leading-none tracking-tighter">주문 내역</h1>

            {errorMessage && <p className="mb-5 text-sm text-[#b23b2f]" role="alert">{errorMessage}</p>}

            {orders.length === 0 ? (
                <div className="grid min-h-80 place-content-center justify-items-center border-y border-line text-center">
                    <ReceiptText className="mb-4 size-9 text-muted" />
                    <strong>아직 주문 내역이 없습니다.</strong>
                    <Link className="mt-4 text-xs underline" to="/">상품 둘러보기</Link>
                </div>
            ) : (
                <div className="border-t border-ink">
                    {orders.map((order) => (
                        <article className="grid gap-4 border-b border-line py-6 min-[601px]:grid-cols-[1fr_auto] min-[601px]:items-center" key={order.orderId}>
                            <div>
                                <div className="flex flex-wrap items-center gap-3">
                                    <strong>주문 #{order.orderId}</strong>
                                    <span className="bg-[#eef0df] px-2 py-1 text-[10px] font-extrabold text-[#66751c]">{getOrderStatusLabel(order.status)}</span>
                                </div>
                                <p className="mt-2 text-xs text-muted">{formatOrderDate(order.createdAt)} · {order.items.map((item) => item.productName).join(', ')}</p>
                            </div>
                            <div className="flex items-center gap-5 min-[601px]:justify-end">
                                <b>{formatPrice(order.totalAmount)}</b>
                                <Link className="text-xs underline" to={`/orders/${order.orderId}/result`}>상세 보기</Link>
                            </div>
                        </article>
                    ))}
                </div>
            )}
        </section>
    )
}
