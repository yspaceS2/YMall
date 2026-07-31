import { LoaderCircle, Star, Trash2, X } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/client'
import {
    createReturnRequest,
    getOrder,
    getOrders,
    getRefunds,
    requestRefund,
} from '../api/orders'
import { createReview, deleteReview, getAllMyReviews, updateReview } from '../api/reviews'
import { RefundDialog } from '../components/RefundDialog'
import { ReturnRequestDialog } from '../components/ReturnRequestDialog'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { PageState } from '../components/ui/PageState'
import type {
    Order,
    OrderItem,
    PaymentRefund,
    PaymentRefundRequest,
    ReturnRequestCreateRequest,
} from '../types/order'
import type { Review } from '../types/review'
import { formatOrderDate, getOrderStatusLabel } from '../utils/order'
import { formatPrice } from '../utils/product'

interface ReviewEditorState {
    orderItemId: number
    reviewId?: number
    rating: number
    content: string
}

interface ReturnTarget {
    orderId: number
    item: OrderItem
}

export function OrderHistoryPage() {
    const [orders, setOrders] = useState<Order[]>([])
    const [reviews, setReviews] = useState<Review[]>([])
    const [editor, setEditor] = useState<ReviewEditorState | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isLoadingMore, setIsLoadingMore] = useState(false)
    const [isSavingReview, setIsSavingReview] = useState(false)
    const [nextPage, setNextPage] = useState(2)
    const [hasNext, setHasNext] = useState(false)
    const [errorMessage, setErrorMessage] = useState('')
    const [successMessage, setSuccessMessage] = useState('')
    const [reviewToDelete, setReviewToDelete] = useState<Review | null>(null)
    const [refundOrder, setRefundOrder] = useState<Order | null>(null)
    const [refunds, setRefunds] = useState<PaymentRefund[]>([])
    const [isLoadingRefunds, setIsLoadingRefunds] = useState(false)
    const [isRefunding, setIsRefunding] = useState(false)
    const [refundError, setRefundError] = useState('')
    const [returnTarget, setReturnTarget] = useState<ReturnTarget | null>(null)
    const [returnError, setReturnError] = useState('')
    const [isRequestingReturn, setIsRequestingReturn] = useState(false)
    const [retryKey, setRetryKey] = useState(0)
    const loadMoreControllerRef = useRef<AbortController | null>(null)
    const reviewsByOrderItemId = useMemo(
        () => new Map(reviews.map((review) => [review.orderItemId, review])),
        [reviews],
    )

    useEffect(() => {
        const controller = new AbortController()
        Promise.all([
            getOrders(1, 20, controller.signal),
            getAllMyReviews(controller.signal),
        ])
            .then(([orderPage, myReviews]) => {
                setOrders(orderPage.content)
                setHasNext(orderPage.hasNext)
                setReviews(myReviews)
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

    function openReviewEditor(item: OrderItem, review?: Review) {
        setErrorMessage('')
        setSuccessMessage('')
        setEditor({
            orderItemId: item.orderItemId,
            reviewId: review?.reviewId,
            rating: review?.rating ?? 5,
            content: review?.content ?? '',
        })
    }

    async function saveReview(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        if (!editor || !editor.content.trim()) return
        setErrorMessage('')
        setSuccessMessage('')
        setIsSavingReview(true)
        try {
            const savedReview = editor.reviewId
                ? await updateReview(editor.reviewId, {
                    rating: editor.rating,
                    content: editor.content.trim(),
                })
                : await createReview({
                    orderItemId: editor.orderItemId,
                    rating: editor.rating,
                    content: editor.content.trim(),
                })
            setReviews((current) => editor.reviewId
                ? current.map((review) => review.reviewId === savedReview.reviewId ? savedReview : review)
                : [savedReview, ...current])
            setSuccessMessage(editor.reviewId ? '리뷰가 수정되었습니다.' : '리뷰가 등록되었습니다.')
            setEditor(null)
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '리뷰를 저장하지 못했습니다.')
        } finally {
            setIsSavingReview(false)
        }
    }

    async function removeReview(review: Review) {
        setErrorMessage('')
        setSuccessMessage('')
        setIsSavingReview(true)
        try {
            await deleteReview(review.reviewId)
            setReviews((current) => current.filter((item) => item.reviewId !== review.reviewId))
            if (editor?.reviewId === review.reviewId) setEditor(null)
            setReviewToDelete(null)
            setSuccessMessage('리뷰가 삭제되었습니다.')
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '리뷰를 삭제하지 못했습니다.')
        } finally {
            setIsSavingReview(false)
        }
    }

    async function openRefundDialog(order: Order) {
        setRefundOrder(order)
        setRefunds([])
        setRefundError('')
        setIsLoadingRefunds(true)
        try {
            setRefunds(await getRefunds(order.orderId))
        } catch (error) {
            setRefundError(error instanceof ApiError
                ? error.message
                : '환불 내역을 불러오지 못했습니다.')
        } finally {
            setIsLoadingRefunds(false)
        }
    }

    async function submitRefund(request: PaymentRefundRequest) {
        if (!refundOrder) return false
        setRefundError('')
        setIsRefunding(true)
        try {
            await requestRefund(refundOrder.orderId, request)
            const [updatedOrder, updatedRefunds] = await Promise.all([
                getOrder(refundOrder.orderId),
                getRefunds(refundOrder.orderId),
            ])
            setRefundOrder(updatedOrder)
            setRefunds(updatedRefunds)
            setOrders((current) => current.map((order) =>
                order.orderId === updatedOrder.orderId ? updatedOrder : order
            ))
            setSuccessMessage('환불 요청이 처리되었습니다.')
            return true
        } catch (error) {
            setRefundError(error instanceof ApiError
                ? error.message
                : '환불 요청을 처리하지 못했습니다.')
            return false
        } finally {
            setIsRefunding(false)
        }
    }

    async function submitReturn(request: ReturnRequestCreateRequest) {
        if (!returnTarget) return false
        setReturnError('')
        setIsRequestingReturn(true)
        try {
            await createReturnRequest(returnTarget.orderId, request)
            setSuccessMessage('반품 신청이 접수되었습니다.')
            return true
        } catch (error) {
            setReturnError(
                error instanceof ApiError
                    ? error.message
                    : '반품 신청을 처리하지 못했습니다.',
            )
            return false
        } finally {
            setIsRequestingReturn(false)
        }
    }

    if (isLoading) {
        return <PageState variant="loading" title="주문 내역을 불러오는 중입니다" description="잠시만 기다려 주세요." />
    }

    if (errorMessage && orders.length === 0) {
        return <PageState variant="error" title="주문 내역을 불러오지 못했습니다" description={errorMessage} action={<button className="border border-ink bg-white px-5 py-2.5 text-xs font-bold" type="button" onClick={() => { setErrorMessage(''); setIsLoading(true); setRetryKey((value) => value + 1) }}>다시 시도</button>} />
    }

    return (
        <section className="mx-auto max-w-260 px-4 py-14 min-[601px]:px-8 min-[601px]:py-20">
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">MY ORDERS</p>
            <h1 className="mb-10 font-serif text-[clamp(42px,6vw,68px)] leading-none tracking-tighter">주문 내역</h1>

            {errorMessage && <FeedbackMessage className="mb-5" tone="error">{errorMessage}</FeedbackMessage>}
            {successMessage && <FeedbackMessage className="mb-5" tone="success">{successMessage}</FeedbackMessage>}

            {orders.length === 0 ? (
                <PageState variant="empty" title="아직 주문 내역이 없습니다" description="마음에 드는 상품을 찾아 첫 주문을 시작해 보세요." action={<Link className="border border-ink bg-white px-5 py-2.5 text-xs font-bold" to="/">상품 둘러보기</Link>} />
            ) : (
                <div className="border-t border-ink">
                    {orders.map((order) => (
                        <article className="border-b border-line py-7" key={order.orderId}>
                            <div className="flex flex-wrap items-center justify-between gap-4">
                                <div>
                                    <div className="flex flex-wrap items-center gap-3">
                                        <strong>주문 #{order.orderId}</strong>
                                        <span className="bg-[#eef0df] px-2 py-1 text-[10px] font-extrabold text-[#66751c]">{getOrderStatusLabel(order.status)}</span>
                                    </div>
                                    <p className="mt-2 text-xs text-muted">{formatOrderDate(order.createdAt)}</p>
                                </div>
                                <div className="flex items-center gap-5">
                                    <b>{formatPrice(order.totalAmount)}</b>
                                    {order.refundSupported && (order.status === 'PAID'
                                        || order.status === 'PARTIALLY_REFUNDED'
                                        || order.status === 'REFUNDED') && (
                                        <button
                                            className="text-xs underline"
                                            type="button"
                                            onClick={() => void openRefundDialog(order)}
                                        >
                                            환불 신청·내역
                                        </button>
                                    )}
                                    <Link className="text-xs underline" to={`/orders/${order.orderId}/result`}>상세 보기</Link>
                                </div>
                            </div>

                            <div className="mt-6 divide-y divide-line border-y border-line">
                                {order.items.map((item) => {
                                    const review = reviewsByOrderItemId.get(item.orderItemId)
                                    const isEditing = editor?.orderItemId === item.orderItemId
                                    return (
                                        <div className="py-4" key={item.orderItemId}>
                                            <div className="flex flex-wrap items-center justify-between gap-3">
                                                <div>
                                                    <Link className="text-sm font-bold hover:underline" to={`/products/${item.productId}`}>
                                                        {item.productName}
                                                    </Link>
                                                    <p className="mt-1 text-xs text-muted">
                                                        {item.quantity}개 · {formatPrice(item.totalPrice)} · {fulfillmentLabel(item.fulfillmentStatus)}
                                                    </p>
                                                </div>
                                                {item.fulfillmentStatus === 'DELIVERED' && (
                                                    <div className="flex gap-2">
                                                        {item.refundedQuantity < item.quantity && (
                                                            <button
                                                                className="border border-ink bg-surface px-3 py-2 text-[11px] font-bold"
                                                                type="button"
                                                                onClick={() => {
                                                                    setReturnError('')
                                                                    setReturnTarget({
                                                                        orderId: order.orderId,
                                                                        item,
                                                                    })
                                                                }}
                                                            >
                                                                반품 신청
                                                            </button>
                                                        )}
                                                        <button
                                                            className="border border-ink bg-white px-3 py-2 text-[11px] font-bold"
                                                            type="button"
                                                            onClick={() => openReviewEditor(item, review)}
                                                        >
                                                            {review ? '리뷰 수정' : '리뷰 작성'}
                                                        </button>
                                                        {review && (
                                                            <button
                                                                className="grid size-8.5 place-items-center border border-line bg-white text-[#b23b2f] disabled:opacity-50"
                                                                type="button"
                                                                disabled={isSavingReview}
                                                                onClick={() => setReviewToDelete(review)}
                                                                aria-label="리뷰 삭제"
                                                            >
                                                                <Trash2 className="size-3.5" />
                                                            </button>
                                                        )}
                                                    </div>
                                                )}
                                            </div>
                                            {review && !isEditing && (
                                                <div className="mt-3 rounded-sm bg-[#f4f4ed] p-4">
                                                    <p className="text-xs tracking-wider text-[#849b21]">{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}</p>
                                                    <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-[#55554f]">{review.content}</p>
                                                </div>
                                            )}
                                            {isEditing && editor && (
                                                <form className="mt-4 border border-line bg-white p-4" onSubmit={saveReview}>
                                                    <div className="mb-3 flex items-center justify-between">
                                                        <strong className="text-sm">{editor.reviewId ? '리뷰 수정' : '리뷰 작성'}</strong>
                                                        <button className="border-0 bg-transparent p-1" type="button" onClick={() => setEditor(null)} aria-label="작성 취소">
                                                            <X className="size-4" />
                                                        </button>
                                                    </div>
                                                    <div className="mb-3 flex gap-1" aria-label="평점 선택">
                                                        {[1, 2, 3, 4, 5].map((rating) => (
                                                            <button
                                                                className="border-0 bg-transparent p-0.5"
                                                                type="button"
                                                                key={rating}
                                                                onClick={() => setEditor((current) => current ? { ...current, rating } : current)}
                                                                aria-label={`${rating}점`}
                                                            >
                                                                <Star className={`size-5 ${rating <= editor.rating ? 'text-[#849b21]' : 'text-[#d8d8d0]'}`} fill="currentColor" />
                                                            </button>
                                                        ))}
                                                    </div>
                                                    <textarea
                                                        className="min-h-28 w-full resize-y border border-line p-3 text-sm outline-none focus:border-ink"
                                                        value={editor.content}
                                                        maxLength={2000}
                                                        required
                                                        onChange={(event) => setEditor((current) => current ? { ...current, content: event.target.value } : current)}
                                                        placeholder="상품을 사용한 경험을 작성해 주세요."
                                                    />
                                                    <div className="mt-3 flex items-center justify-between gap-3">
                                                        <span className="text-[11px] text-muted">{editor.content.length}/2000</span>
                                                        <button
                                                            className="h-9 bg-ink px-5 text-xs font-bold text-white disabled:opacity-50"
                                                            type="submit"
                                                            disabled={isSavingReview || !editor.content.trim()}
                                                        >
                                                            {isSavingReview ? '저장 중...' : '저장'}
                                                        </button>
                                                    </div>
                                                </form>
                                            )}
                                        </div>
                                    )
                                })}
                            </div>
                        </article>
                    ))}
                    {hasNext && (
                        <button
                            className="mx-auto mt-8 grid h-11 min-w-36 place-items-center border border-ink bg-white px-6 text-xs font-bold disabled:opacity-50"
                            type="button"
                            disabled={isLoadingMore}
                            onClick={loadMore}
                        >
                            {isLoadingMore ? <LoaderCircle className="size-4 animate-spin" /> : '주문 더 보기'}
                        </button>
                    )}
                </div>
            )}
            <ConfirmDialog
                open={reviewToDelete !== null}
                title="리뷰를 삭제할까요?"
                description="삭제한 리뷰는 복구할 수 없습니다. 상품을 다시 구매하지 않으면 새 리뷰를 작성할 수 없을 수 있습니다."
                confirmLabel="리뷰 삭제"
                isPending={isSavingReview}
                onCancel={() => setReviewToDelete(null)}
                onConfirm={() => {
                    if (reviewToDelete) void removeReview(reviewToDelete)
                }}
            />
            <RefundDialog
                key={refundOrder?.orderId ?? 'closed'}
                open={refundOrder !== null}
                orderId={refundOrder?.orderId ?? null}
                items={refundOrder?.items ?? []}
                refunds={refunds}
                isLoadingHistory={isLoadingRefunds}
                isSubmitting={isRefunding}
                errorMessage={refundError}
                onClose={() => {
                    if (!isRefunding) setRefundOrder(null)
                }}
                onSubmit={submitRefund}
            />
            <ReturnRequestDialog
                key={returnTarget?.item.orderItemId ?? 'closed'}
                open={returnTarget !== null}
                item={returnTarget?.item ?? null}
                isSubmitting={isRequestingReturn}
                errorMessage={returnError}
                onClose={() => {
                    if (!isRequestingReturn) setReturnTarget(null)
                }}
                onSubmit={submitReturn}
            />
        </section>
    )
}

function fulfillmentLabel(status: OrderItem['fulfillmentStatus']) {
    return {
        PENDING: '상품 준비 전',
        PREPARING: '상품 준비 중',
        SHIPPED: '배송 중',
        DELIVERED: '배송 완료',
    }[status]
}
