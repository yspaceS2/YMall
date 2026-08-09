import { CircleCheck, CircleX, Clock3 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { getOrder } from '../api/orders'
import type { Order } from '../types/order'
import { formatOrderDate, getOrderStatusLabel } from '../utils/order'
import { formatPrice, resolveImageUrl } from '../utils/product'

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
                setErrorMessage(error instanceof ApiError ? error.message : '주문 상세를 불러오지 못했습니다.')
            })
        return () => controller.abort()
    }, [isValidOrderId, orderId])

    if (!isValidOrderId) {
        return <div className="grid min-h-100 place-content-center gap-4 text-center text-sm"><p>올바르지 않은 주문 번호입니다.</p><Link className="text-xs underline" to="/mypage/orders">주문 내역으로</Link></div>
    }

    if (!order) {
        return <div className="grid min-h-100 place-content-center gap-4 text-center text-sm"><p>{errorMessage || '주문 상세를 확인하고 있습니다.'}</p>{errorMessage && <Link className="text-xs underline" to="/mypage/orders">주문 내역으로</Link>}</div>
    }

    const isSuccess = order.status === 'PAID'
    const isFailed = order.status === 'PAYMENT_FAILED'
    const canPay = order.status === 'PENDING_PAYMENT' || isFailed
    const Icon = isSuccess ? CircleCheck : isFailed || order.status === 'CANCELED' ? CircleX : Clock3

    return (
        <section className="mx-auto max-w-240 px-4 py-14 min-[601px]:px-8 min-[601px]:py-20">
            <div className="border-b border-line pb-9 text-center">
                <Icon className={`mx-auto mb-5 size-11 ${isSuccess ? 'text-success' : isFailed ? 'text-danger' : 'text-muted'}`} />
                <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-accent">ORDER DETAIL</p>
                <h1 className="font-serif text-[clamp(38px,6vw,58px)] leading-none tracking-tighter">{getOrderStatusLabel(order.status)}</h1>
                <p className="mt-5 text-sm text-muted">주문 #{order.orderId} · {formatOrderDate(order.createdAt)}</p>
                {isFailed && <p className="mt-3 text-sm text-danger">결제에 실패했습니다. 주문을 유지한 채 다시 시도할 수 있습니다.</p>}
            </div>

            <div className="divide-y divide-line">
                {order.items.map((item) => (
                    <article className="flex gap-4 py-6 min-[601px]:items-center" key={item.orderItemId}>
                        <Link
                            className="size-22 shrink-0 overflow-hidden bg-subtle min-[601px]:size-28"
                            to={`/products/${item.productId}`}
                            aria-label={`${item.productName} 상품 보기`}
                        >
                            {item.thumbnailUrl ? (
                                <img className="size-full object-cover" src={resolveImageUrl(item.thumbnailUrl)} alt="" />
                            ) : (
                                <span className="grid size-full place-items-center text-[10px] font-bold tracking-widest text-muted">YMALL</span>
                            )}
                        </Link>
                        <div className="min-w-0 flex-1">
                            <Link className="font-bold hover:underline" to={`/products/${item.productId}`}>
                                {item.productName}
                            </Link>
                            <p className="mt-2 text-xs text-muted">
                                {item.quantity}개 · {getFulfillmentStatusLabel(item.fulfillmentStatus)}
                            </p>
                        </div>
                        <strong className="shrink-0 text-sm">{formatPrice(item.totalPrice)}</strong>
                    </article>
                ))}
            </div>

            <div className="grid gap-6 border-y border-line py-7 min-[701px]:grid-cols-2">
                <div>
                    <h2 className="text-sm font-extrabold">배송지</h2>
                    {order.deliveryAddress ? (
                        <div className="mt-3 space-y-1 text-sm leading-6 text-muted">
                            <p>{order.deliveryAddress.recipientName} · {order.deliveryAddress.recipientPhone}</p>
                            <p>
                                ({order.deliveryAddress.postalCode}) {order.deliveryAddress.roadAddress}
                                {order.deliveryAddress.detailAddress ? ` ${order.deliveryAddress.detailAddress}` : ''}
                            </p>
                        </div>
                    ) : (
                        <p className="mt-3 text-sm text-muted">등록된 배송지 정보가 없습니다.</p>
                    )}
                </div>
                <dl className="space-y-2 text-sm">
                    <div className="flex justify-between gap-4">
                        <dt className="text-muted">상품 금액</dt>
                        <dd>{formatPrice(order.productAmount)}</dd>
                    </div>
                    <div className="flex justify-between gap-4">
                        <dt className="text-muted">배송비</dt>
                        <dd>{formatPrice(order.shippingFee)}</dd>
                    </div>
                    <div className="flex justify-between gap-4 border-t border-line pt-3 font-extrabold">
                        <dt>총 결제 금액</dt>
                        <dd>{formatPrice(order.totalAmount)}</dd>
                    </div>
                </dl>
            </div>

            <div className="mt-8 flex flex-wrap justify-center gap-3">
                {canPay && (
                    <Link className="bg-ink px-6 py-3 text-xs font-bold text-white" to={`/orders/${order.orderId}/payment`}>
                        {isFailed ? '결제 다시 시도' : '결제 계속하기'}
                    </Link>
                )}
                <Link className="border border-ink px-6 py-3 text-xs font-bold" to="/mypage/orders">주문 내역 보기</Link>
                <Link className="px-6 py-3 text-xs underline" to="/">쇼핑 계속하기</Link>
            </div>
        </section>
    )
}

function getFulfillmentStatusLabel(status: Order['items'][number]['fulfillmentStatus']) {
    return {
        PENDING: '상품 준비 전',
        PREPARING: '상품 준비 중',
        SHIPPED: '배송 중',
        DELIVERED: '배송 완료',
    }[status]
}
