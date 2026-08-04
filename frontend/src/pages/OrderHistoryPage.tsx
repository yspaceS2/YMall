import { LoaderCircle } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/client'
import { getOrders } from '../api/orders'
import { OrderStatusBadge } from '../components/order/OrderStatusBadge'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { PageState } from '../components/ui/PageState'
import type { Order } from '../types/order'
import { formatOrderDate } from '../utils/order'
import { formatPrice, resolveImageUrl } from '../utils/product'

export function OrderHistoryPage() {
    const [orders, setOrders] = useState<Order[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [isLoadingMore, setIsLoadingMore] = useState(false)
    const [nextPage, setNextPage] = useState(2)
    const [hasNext, setHasNext] = useState(false)
    const [errorMessage, setErrorMessage] = useState('')
    const [retryKey, setRetryKey] = useState(0)
    const loadMoreControllerRef = useRef<AbortController | null>(null)

    useEffect(() => {
        const controller = new AbortController()
        getOrders(1, 20, controller.signal)
            .then((orderPage) => {
                setOrders(orderPage.content)
                setHasNext(orderPage.hasNext)
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError ? error.message : '주문 내역을 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => {
            controller.abort()
            loadMoreControllerRef.current?.abort()
        }
    }, [retryKey])

    async function loadMore() {
        if (!hasNext || isLoadingMore) return
        const controller = new AbortController()
        loadMoreControllerRef.current = controller
        setErrorMessage('')
        setIsLoadingMore(true)
        try {
            const response = await getOrders(nextPage, 20, controller.signal)
            setOrders((current) => [...current, ...response.content])
            setHasNext(response.hasNext)
            setNextPage((current) => current + 1)
        } catch (error) {
            if (error instanceof Error && error.name === 'AbortError') return
            setErrorMessage(error instanceof ApiError ? error.message : '주문 내역을 불러오지 못했습니다.')
        } finally {
            if (!controller.signal.aborted) setIsLoadingMore(false)
        }
    }

    if (isLoading) {
        return <PageState variant="loading" title="주문 내역을 불러오는 중입니다" description="잠시만 기다려 주세요." />
    }

    if (errorMessage && orders.length === 0) {
        return (
            <PageState
                variant="error"
                title="주문 내역을 불러오지 못했습니다"
                description={errorMessage}
                action={(
                    <button
                        className="border border-ink bg-surface px-5 py-2.5 text-xs font-bold"
                        type="button"
                        onClick={() => {
                            setErrorMessage('')
                            setIsLoading(true)
                            setRetryKey((value) => value + 1)
                        }}
                    >
                        다시 시도
                    </button>
                )}
            />
        )
    }

    return (
        <section className="mx-auto max-w-260 px-4 py-14 min-[601px]:px-8 min-[601px]:py-20">
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-accent">MY ORDERS</p>
            <h1 className="mb-10 font-serif text-[clamp(42px,6vw,68px)] leading-none tracking-tighter">주문 내역</h1>

            {errorMessage && <FeedbackMessage className="mb-5" tone="error">{errorMessage}</FeedbackMessage>}
            {orders.length === 0 ? (
                <PageState
                    variant="empty"
                    title="아직 주문 내역이 없습니다"
                    description="마음에 드는 상품을 찾아 첫 주문을 시작해 보세요."
                    action={<Link className="border border-ink bg-surface px-5 py-2.5 text-xs font-bold" to="/">상품 둘러보기</Link>}
                />
            ) : (
                <div className="border-t border-ink">
                    {orders.map((order) => {
                        const representativeItem = order.items[0]
                        const additionalItemCount = Math.max(order.items.length - 1, 0)
                        const orderTitle = representativeItem
                            ? `${representativeItem.productName}${additionalItemCount > 0 ? ` 외 ${additionalItemCount}개` : ''}`
                            : `주문 #${order.orderId}`
                        const detailUrl = `/mypage/orders/${order.orderId}`

                        return (
                            <article className="border-b border-line py-7" key={order.orderId}>
                                <div className="grid items-center gap-4 min-[701px]:grid-cols-[76px_minmax(0,1fr)_auto]">
                                    <Link
                                        className="grid size-19 place-items-center overflow-hidden bg-subtle"
                                        to={detailUrl}
                                        aria-label={`${orderTitle} 주문 상세 보기`}
                                    >
                                        {representativeItem?.thumbnailUrl ? (
                                            <img
                                                className="size-full object-cover"
                                                src={resolveImageUrl(representativeItem.thumbnailUrl)}
                                                alt=""
                                            />
                                        ) : (
                                            <span className="font-serif text-[10px] font-bold tracking-[.14em] text-muted">YMALL</span>
                                        )}
                                    </Link>
                                    <div className="min-w-0">
                                        <div className="flex flex-wrap items-center gap-3">
                                            <Link className="truncate font-bold hover:underline" to={detailUrl}>
                                                {orderTitle}
                                            </Link>
                                            <OrderStatusBadge className="shrink-0" status={order.status} />
                                        </div>
                                        <p className="mt-2 text-xs text-muted">
                                            주문 #{order.orderId} · {formatOrderDate(order.createdAt)}
                                        </p>
                                    </div>
                                    <div className="flex flex-wrap items-center gap-5 min-[701px]:justify-end">
                                        <b>{formatPrice(order.totalAmount)}</b>
                                        <Link className="text-xs font-bold underline underline-offset-4" to={detailUrl}>
                                            상세 보기
                                        </Link>
                                    </div>
                                </div>
                            </article>
                        )
                    })}
                    {hasNext && (
                        <button
                            className="mx-auto mt-8 grid h-11 min-w-36 place-items-center border border-ink bg-surface px-6 text-xs font-bold disabled:opacity-50"
                            type="button"
                            disabled={isLoadingMore}
                            onClick={loadMore}
                        >
                            {isLoadingMore ? <LoaderCircle className="size-4 animate-spin" /> : '주문 더 보기'}
                        </button>
                    )}
                </div>
            )}
        </section>
    )
}
