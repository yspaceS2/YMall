import { ArrowLeft, Star, Trash2, X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import {
    createReturnRequest,
    getOrder,
    getRefunds,
    getReturnRequests,
    requestRefund,
} from '../api/orders'
import { createReview, deleteReview, getAllMyReviews, updateReview } from '../api/reviews'
import { RefundDialog } from '../components/RefundDialog'
import { ReturnRequestDialog } from '../components/ReturnRequestDialog'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { PageState } from '../components/ui/PageState'
import { useToast } from '../toast/useToast'
import type {
    Order,
    OrderItem,
    PaymentRefund,
    PaymentRefundRequest,
    ReturnRequest,
    ReturnRequestCreateRequest,
} from '../types/order'
import type { Review } from '../types/review'
import { formatOrderDate, getOrderStatusLabel } from '../utils/order'
import { formatPrice, resolveImageUrl } from '../utils/product'

interface ReviewEditorState {
    orderItemId: number
    reviewId?: number
    rating: number
    content: string
}

export function OrderDetailPage() {
    const { showToast } = useToast()
    const { orderId: orderIdParam } = useParams()
    const orderId = Number(orderIdParam)
    const isValidOrderId = Number.isInteger(orderId) && orderId > 0
    const [order, setOrder] = useState<Order | null>(null)
    const [reviews, setReviews] = useState<Review[]>([])
    const [editor, setEditor] = useState<ReviewEditorState | null>(null)
    const [reviewToDelete, setReviewToDelete] = useState<Review | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isSavingReview, setIsSavingReview] = useState(false)
    const [errorMessage, setErrorMessage] = useState('')
    const [retryKey, setRetryKey] = useState(0)
    const [refundDialogOpen, setRefundDialogOpen] = useState(false)
    const [refunds, setRefunds] = useState<PaymentRefund[]>([])
    const [isLoadingRefunds, setIsLoadingRefunds] = useState(false)
    const [isRefunding, setIsRefunding] = useState(false)
    const [refundError, setRefundError] = useState('')
    const [returnRequests, setReturnRequests] = useState<ReturnRequest[]>([])
    const [returnItem, setReturnItem] = useState<OrderItem | null>(null)
    const [returnError, setReturnError] = useState('')
    const [isRequestingReturn, setIsRequestingReturn] = useState(false)
    const reviewsByOrderItemId = useMemo(
        () => new Map(reviews.map((review) => [review.orderItemId, review])),
        [reviews],
    )

    useEffect(() => {
        if (!isValidOrderId) return
        const controller = new AbortController()
        Promise.all([
            getOrder(orderId, controller.signal),
            getAllMyReviews(controller.signal),
            getReturnRequests(orderId, controller.signal),
        ])
            .then(([orderResponse, reviewResponse, returnResponse]) => {
                setOrder(orderResponse)
                setReviews(reviewResponse)
                setReturnRequests(returnResponse)
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError ? error.message : '주문 상세를 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [isValidOrderId, orderId, retryKey])

    function openReviewEditor(item: OrderItem, review?: Review) {
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
            showToast(editor.reviewId ? '리뷰가 수정되었습니다.' : '리뷰가 등록되었습니다.', 'success')
            setEditor(null)
        } catch (error) {
            showToast(error instanceof ApiError ? error.message : '리뷰를 저장하지 못했습니다.', 'error')
        } finally {
            setIsSavingReview(false)
        }
    }

    async function removeReview(review: Review) {
        setIsSavingReview(true)
        try {
            await deleteReview(review.reviewId)
            setReviews((current) => current.filter((item) => item.reviewId !== review.reviewId))
            if (editor?.reviewId === review.reviewId) setEditor(null)
            setReviewToDelete(null)
            showToast('리뷰가 삭제되었습니다.', 'success')
        } catch (error) {
            showToast(error instanceof ApiError ? error.message : '리뷰를 삭제하지 못했습니다.', 'error')
        } finally {
            setIsSavingReview(false)
        }
    }

    async function openRefundDialog() {
        if (!order) return
        setRefundDialogOpen(true)
        setRefunds([])
        setRefundError('')
        setIsLoadingRefunds(true)
        try {
            setRefunds(await getRefunds(order.orderId))
        } catch (error) {
            setRefundError(error instanceof ApiError ? error.message : '환불 내역을 불러오지 못했습니다.')
        } finally {
            setIsLoadingRefunds(false)
        }
    }

    async function submitRefund(request: PaymentRefundRequest) {
        if (!order) return false
        setRefundError('')
        setIsRefunding(true)
        try {
            await requestRefund(order.orderId, request)
            const [updatedOrder, updatedRefunds] = await Promise.all([
                getOrder(order.orderId),
                getRefunds(order.orderId),
            ])
            setOrder(updatedOrder)
            setRefunds(updatedRefunds)
            showToast('환불 요청이 처리되었습니다.', 'success')
            return true
        } catch (error) {
            setRefundError(error instanceof ApiError ? error.message : '환불 요청을 처리하지 못했습니다.')
            return false
        } finally {
            setIsRefunding(false)
        }
    }

    async function submitReturnRequest(request: ReturnRequestCreateRequest) {
        if (!order) return false
        setReturnError('')
        setIsRequestingReturn(true)
        try {
            const created = await createReturnRequest(order.orderId, request)
            setReturnRequests((current) => [created, ...current])
            showToast('반품을 신청했습니다.', 'success')
            return true
        } catch (error) {
            setReturnError(error instanceof ApiError ? error.message : '반품을 신청하지 못했습니다.')
            return false
        } finally {
            setIsRequestingReturn(false)
        }
    }

    if (!isValidOrderId) {
        return (
            <PageState
                variant="error"
                title="주문을 찾을 수 없습니다"
                description="올바르지 않은 주문 주소입니다."
                action={<Link className="border border-ink bg-surface px-5 py-2.5 text-xs font-bold" to="/mypage/orders">주문 내역으로</Link>}
            />
        )
    }

    if (isLoading) {
        return <PageState variant="loading" title="주문 상세를 불러오는 중입니다" description="잠시만 기다려 주세요." />
    }

    if (!order) {
        return (
            <PageState
                variant="error"
                title="주문 상세를 불러오지 못했습니다"
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

    const representativeItem = order.items[0]
    const additionalItemCount = Math.max(order.items.length - 1, 0)
    const orderTitle = representativeItem
        ? `${representativeItem.productName}${additionalItemCount > 0 ? ` 외 ${additionalItemCount}개` : ''}`
        : `주문 #${order.orderId}`
    const canPay = order.status === 'PENDING_PAYMENT' || order.status === 'PAYMENT_FAILED'

    return (
        <section className="mx-auto max-w-260 px-4 py-12 min-[601px]:px-8 min-[601px]:py-16">
            <Link className="mb-8 inline-flex items-center gap-2 text-xs font-bold text-muted hover:text-ink" to="/mypage/orders">
                <ArrowLeft className="size-4" />
                주문 내역으로
            </Link>

            <header className="border-b border-ink pb-8">
                <div className="flex flex-wrap items-center gap-3">
                    <p className="text-[11px] font-extrabold tracking-[.18em] text-[#71801e] dark:text-[#c9db72]">ORDER DETAIL</p>
                    <span className="bg-[#eef0df] px-2.5 py-1 text-[10px] font-extrabold text-[#66751c] dark:bg-[#29301f] dark:text-[#d3e78a]">
                        {getOrderStatusLabel(order.status)}
                    </span>
                </div>
                <h1 className="mt-3 font-serif text-[clamp(34px,5vw,56px)] leading-tight tracking-tight">{orderTitle}</h1>
                <p className="mt-4 text-xs text-muted">
                    주문 #{order.orderId} · {formatOrderDate(order.createdAt)}
                </p>
            </header>

            {errorMessage && <FeedbackMessage className="mt-6" tone="error">{errorMessage}</FeedbackMessage>}

            <div className="grid gap-10 py-10 min-[901px]:grid-cols-[minmax(0,1fr)_300px]">
                <div>
                    <h2 className="mb-4 text-sm font-extrabold">주문 상품</h2>
                    <div className="divide-y divide-line border-y border-line">
                        {order.items.map((item) => {
                            const review = reviewsByOrderItemId.get(item.orderItemId)
                            const isEditing = editor?.orderItemId === item.orderItemId
                            const itemReturnRequests = returnRequests.filter(
                                (request) => request.orderItemId === item.orderItemId,
                            )
                            const pendingReturn = itemReturnRequests.find(
                                (request) => request.status === 'REQUESTED',
                            )
                            const latestReturn = itemReturnRequests[0]
                            const canRequestReturn = item.fulfillmentStatus === 'DELIVERED'
                                && item.quantity > item.refundedQuantity
                                && !pendingReturn
                            return (
                                <article className="py-5" key={item.orderItemId}>
                                    <div className="grid grid-cols-[76px_minmax(0,1fr)] items-center gap-4 min-[701px]:grid-cols-[76px_minmax(0,1fr)_auto]">
                                        <Link
                                            className="grid size-19 place-items-center overflow-hidden bg-[#e9e9e3] dark:bg-[#30322d]"
                                            to={`/products/${item.productId}`}
                                            aria-label={`${item.productName} 상품 보기`}
                                        >
                                            {item.thumbnailUrl ? (
                                                <img className="size-full object-cover" src={resolveImageUrl(item.thumbnailUrl)} alt="" />
                                            ) : (
                                                <span className="font-serif text-[9px] font-bold tracking-[.12em] text-muted">YMALL</span>
                                            )}
                                        </Link>
                                        <div className="min-w-0">
                                            <Link className="text-sm font-bold hover:underline" to={`/products/${item.productId}`}>
                                                {item.productName}
                                            </Link>
                                            <p className="mt-2 text-xs text-muted">
                                                {item.quantity}개 · {formatPrice(item.totalPrice)}
                                            </p>
                                            <p className="mt-1 text-xs font-bold">
                                                {fulfillmentLabel(item.fulfillmentStatus, order.status)}
                                            </p>
                                        </div>
                                        {item.fulfillmentStatus === 'DELIVERED'
                                            && order.status !== 'CANCELED'
                                            && (
                                            <div className="col-start-2 flex flex-wrap gap-2 min-[701px]:col-start-auto">
                                                <button
                                                    className="border border-ink bg-surface px-3 py-2 text-[11px] font-bold"
                                                    type="button"
                                                    onClick={() => openReviewEditor(item, review)}
                                                >
                                                    {review ? '리뷰 수정' : '리뷰 작성'}
                                                </button>
                                                {review && (
                                                    <button
                                                        className="grid size-8.5 place-items-center border border-line bg-surface text-[#b23b2f] dark:text-[#ffb7ae] disabled:opacity-50"
                                                        type="button"
                                                        disabled={isSavingReview}
                                                        onClick={() => setReviewToDelete(review)}
                                                        aria-label="리뷰 삭제"
                                                    >
                                                        <Trash2 className="size-3.5" />
                                                    </button>
                                                )}
                                                {canRequestReturn && (
                                                    <button
                                                        className="border border-ink bg-surface px-3 py-2 text-[11px] font-bold"
                                                        type="button"
                                                        onClick={() => {
                                                            setReturnError('')
                                                            setReturnItem(item)
                                                        }}
                                                    >
                                                        반품 신청
                                                    </button>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                    {latestReturn && (
                                        <div className="mt-4 border border-line bg-surface p-4 text-xs leading-6 min-[701px]:ml-23">
                                            <div className="flex flex-wrap items-center justify-between gap-2">
                                                <strong>반품 {returnStatusLabel(latestReturn.status)}</strong>
                                                <span className="text-muted">
                                                    {formatOrderDate(latestReturn.requestedAt)}
                                                </span>
                                            </div>
                                            <p className="mt-2 text-muted">
                                                {latestReturn.quantity}개 · {latestReturn.reason}
                                            </p>
                                            {latestReturn.sellerResponse && (
                                                <p className="mt-2">판매자 답변: {latestReturn.sellerResponse}</p>
                                            )}
                                        </div>
                                    )}
                                    {review && !isEditing && (
                                        <div className="mt-4 rounded-sm bg-[#f4f4ed] p-4 dark:bg-[#2c2e29] min-[701px]:ml-23">
                                            <p className="text-xs tracking-wider text-[#849b21]">
                                                {'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}
                                            </p>
                                            <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-[#55554f] dark:text-[#d2d1c9]">
                                                {review.content}
                                            </p>
                                        </div>
                                    )}
                                    {isEditing && editor && (
                                        <form className="mt-4 border border-line bg-surface p-4 min-[701px]:ml-23" onSubmit={saveReview}>
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
                                                className="min-h-28 w-full resize-y border border-line bg-surface p-3 text-sm text-ink outline-none focus:border-ink"
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
                                </article>
                            )
                        })}
                    </div>
                </div>

                <aside className="grid content-start gap-6">
                    <section className="border border-line bg-surface p-5">
                        <h2 className="text-sm font-extrabold">결제 정보</h2>
                        <dl className="mt-5 grid gap-3 text-xs">
                            <div className="flex justify-between gap-4 text-muted">
                                <dt>상품금액</dt>
                                <dd className="text-ink">{formatPrice(order.productAmount)}</dd>
                            </div>
                            <div className="flex justify-between gap-4 text-muted">
                                <dt>배송비</dt>
                                <dd className="text-ink">{formatPrice(order.shippingFee)}</dd>
                            </div>
                            <div className="flex justify-between gap-4 border-t border-line pt-4 text-sm font-extrabold">
                                <dt>총 결제금액</dt>
                                <dd>{formatPrice(order.totalAmount)}</dd>
                            </div>
                        </dl>
                        <div className="mt-5 grid gap-2">
                            {canPay && (
                                <Link className="grid h-11 place-items-center bg-ink text-xs font-bold text-white" to={`/orders/${order.orderId}/payment`}>
                                    결제 계속하기
                                </Link>
                            )}
                            {order.refundSupported
                                && order.status !== 'DELIVERED'
                                && order.status !== 'SHIPPED'
                                && (
                                <button className="h-11 border border-ink text-xs font-bold" type="button" onClick={() => void openRefundDialog()}>
                                    환불 신청·내역
                                </button>
                            )}
                        </div>
                    </section>

                    {order.deliveryAddress && (
                        <section className="border border-line bg-surface p-5">
                            <h2 className="text-sm font-extrabold">배송지 정보</h2>
                            <dl className="mt-5 grid gap-3 text-xs leading-5">
                                <div>
                                    <dt className="text-muted">받는 분</dt>
                                    <dd className="mt-1">{order.deliveryAddress.recipientName}</dd>
                                </div>
                                <div>
                                    <dt className="text-muted">연락처</dt>
                                    <dd className="mt-1">{order.deliveryAddress.recipientPhone}</dd>
                                </div>
                                <div>
                                    <dt className="text-muted">주소</dt>
                                    <dd className="mt-1">
                                        ({order.deliveryAddress.postalCode}) {order.deliveryAddress.roadAddress} {order.deliveryAddress.detailAddress}
                                    </dd>
                                </div>
                            </dl>
                        </section>
                    )}
                </aside>
            </div>

            <ConfirmDialog
                open={reviewToDelete !== null}
                title="리뷰를 삭제할까요?"
                description="삭제한 리뷰는 복구할 수 없습니다."
                confirmLabel="리뷰 삭제"
                isPending={isSavingReview}
                onCancel={() => setReviewToDelete(null)}
                onConfirm={() => {
                    if (reviewToDelete) void removeReview(reviewToDelete)
                }}
            />
            <RefundDialog
                key={refundDialogOpen ? order.orderId : 'closed'}
                open={refundDialogOpen}
                orderId={order.orderId}
                items={order.items}
                refunds={refunds}
                isLoadingHistory={isLoadingRefunds}
                isSubmitting={isRefunding}
                errorMessage={refundError}
                onClose={() => {
                    if (!isRefunding) setRefundDialogOpen(false)
                }}
                onSubmit={submitRefund}
            />
            <ReturnRequestDialog
                key={returnItem?.orderItemId ?? 'closed'}
                open={returnItem !== null}
                item={returnItem}
                isSubmitting={isRequestingReturn}
                errorMessage={returnError}
                onClose={() => {
                    if (!isRequestingReturn) setReturnItem(null)
                }}
                onSubmit={submitReturnRequest}
            />
        </section>
    )
}

function fulfillmentLabel(
    status: OrderItem['fulfillmentStatus'],
    orderStatus: Order['status'],
) {
    if (orderStatus === 'CANCELED') return '취소 완료'
    if (orderStatus === 'REFUNDED') return '환불 완료'
    return {
        PENDING: '상품 준비 전',
        PREPARING: '상품 준비 중',
        SHIPPED: '배송 중',
        DELIVERED: '배송 완료',
    }[status]
}

function returnStatusLabel(status: ReturnRequest['status']) {
    return {
        REQUESTED: '요청 중',
        APPROVED: '승인 완료',
        REJECTED: '거절',
    }[status]
}
