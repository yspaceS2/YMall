import { Star, Trash2, X } from 'lucide-react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import type { ReviewEditorState } from '../../hooks/useOrderDetail'
import type { Order, OrderItem, ReturnRequest } from '../../types/order'
import type { Review } from '../../types/review'
import { formatOrderDate } from '../../utils/order'
import { formatPrice, resolveImageUrl } from '../../utils/product'

interface OrderItemsSectionProps {
    editor: ReviewEditorState | null
    isSavingReview: boolean
    order: Order
    returnRequests: ReturnRequest[]
    reviewsByOrderItemId: Map<number, Review>
    onCancelEditor: () => void
    onChangeEditor: (patch: Partial<Pick<ReviewEditorState, 'rating' | 'content'>>) => void
    onDeleteReview: (review: Review) => void
    onEditReview: (item: OrderItem, review?: Review) => void
    onRequestReturn: (item: OrderItem) => void
    onSaveReview: (event: FormEvent<HTMLFormElement>) => void
}

export function OrderItemsSection({
    editor,
    isSavingReview,
    order,
    returnRequests,
    reviewsByOrderItemId,
    onCancelEditor,
    onChangeEditor,
    onDeleteReview,
    onEditReview,
    onRequestReturn,
    onSaveReview,
}: OrderItemsSectionProps) {
    return (
        <div>
            <h2 className="mb-4 text-sm font-extrabold">주문 상품</h2>
            <div className="divide-y divide-line border-y border-line">
                {order.items.map((item) => (
                    <OrderItemArticle
                        editor={editor}
                        isSavingReview={isSavingReview}
                        item={item}
                        key={item.orderItemId}
                        order={order}
                        returnRequests={returnRequests}
                        review={reviewsByOrderItemId.get(item.orderItemId)}
                        onCancelEditor={onCancelEditor}
                        onChangeEditor={onChangeEditor}
                        onDeleteReview={onDeleteReview}
                        onEditReview={onEditReview}
                        onRequestReturn={onRequestReturn}
                        onSaveReview={onSaveReview}
                    />
                ))}
            </div>
        </div>
    )
}

interface OrderItemArticleProps extends Omit<OrderItemsSectionProps, 'reviewsByOrderItemId'> {
    item: OrderItem
    review?: Review
}

function OrderItemArticle({
    editor,
    isSavingReview,
    item,
    order,
    returnRequests,
    review,
    onCancelEditor,
    onChangeEditor,
    onDeleteReview,
    onEditReview,
    onRequestReturn,
    onSaveReview,
}: OrderItemArticleProps) {
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
        <article className="py-5">
            <div className="grid grid-cols-[76px_minmax(0,1fr)] items-center gap-4 min-[701px]:grid-cols-[76px_minmax(0,1fr)_auto]">
                <Link
                    className="grid size-19 place-items-center overflow-hidden bg-subtle"
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
                            onClick={() => onEditReview(item, review)}
                        >
                            {review ? '리뷰 수정' : '리뷰 작성'}
                        </button>
                        {review && (
                            <button
                                className="grid size-8.5 place-items-center border border-line bg-surface text-danger disabled:opacity-50"
                                type="button"
                                disabled={isSavingReview}
                                onClick={() => onDeleteReview(review)}
                                aria-label="리뷰 삭제"
                            >
                                <Trash2 className="size-3.5" />
                            </button>
                        )}
                        {canRequestReturn && (
                            <button
                                className="border border-ink bg-surface px-3 py-2 text-[11px] font-bold"
                                type="button"
                                onClick={() => onRequestReturn(item)}
                            >
                                반품 신청
                            </button>
                        )}
                    </div>
                )}
            </div>
            {latestReturn && <ReturnRequestSummary request={latestReturn} />}
            {review && !isEditing && <ReviewSummary review={review} />}
            {isEditing && editor && (
                <ReviewEditor
                    editor={editor}
                    isSaving={isSavingReview}
                    onCancel={onCancelEditor}
                    onChange={onChangeEditor}
                    onSubmit={onSaveReview}
                />
            )}
        </article>
    )
}

function ReturnRequestSummary({ request }: { request: ReturnRequest }) {
    return (
        <div className="mt-4 border border-line bg-surface p-4 text-xs leading-6 min-[701px]:ml-23">
            <div className="flex flex-wrap items-center justify-between gap-2">
                <strong>반품 {returnStatusLabel(request.status)}</strong>
                <span className="text-muted">{formatOrderDate(request.requestedAt)}</span>
            </div>
            <p className="mt-2 text-muted">{request.quantity}개 · {request.reason}</p>
            {request.sellerResponse && (
                <p className="mt-2">판매자 답변: {request.sellerResponse}</p>
            )}
        </div>
    )
}

function ReviewSummary({ review }: { review: Review }) {
    return (
        <div className="mt-4 rounded-sm bg-paper p-4 min-[701px]:ml-23">
            <p className="text-xs tracking-wider text-accent">
                {'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}
            </p>
            <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-muted">
                {review.content}
            </p>
        </div>
    )
}

interface ReviewEditorProps {
    editor: ReviewEditorState
    isSaving: boolean
    onCancel: () => void
    onChange: (patch: Partial<Pick<ReviewEditorState, 'rating' | 'content'>>) => void
    onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

function ReviewEditor({ editor, isSaving, onCancel, onChange, onSubmit }: ReviewEditorProps) {
    return (
        <form className="mt-4 border border-line bg-surface p-4 min-[701px]:ml-23" onSubmit={onSubmit}>
            <div className="mb-3 flex items-center justify-between">
                <strong className="text-sm">{editor.reviewId ? '리뷰 수정' : '리뷰 작성'}</strong>
                <button className="border-0 bg-transparent p-1" type="button" onClick={onCancel} aria-label="작성 취소">
                    <X className="size-4" />
                </button>
            </div>
            <div className="mb-3 flex gap-1" aria-label="평점 선택">
                {[1, 2, 3, 4, 5].map((rating) => (
                    <button
                        className="border-0 bg-transparent p-0.5"
                        type="button"
                        key={rating}
                        onClick={() => onChange({ rating })}
                        aria-label={`${rating}점`}
                    >
                        <Star className={`size-5 ${rating <= editor.rating ? 'text-accent' : 'text-line'}`} fill="currentColor" />
                    </button>
                ))}
            </div>
            <textarea
                className="min-h-28 w-full resize-y border border-line bg-surface p-3 text-sm text-ink outline-none focus:border-ink"
                value={editor.content}
                maxLength={2000}
                required
                onChange={(event) => onChange({ content: event.target.value })}
                placeholder="상품을 사용한 경험을 작성해 주세요."
            />
            <div className="mt-3 flex items-center justify-between gap-3">
                <span className="text-[11px] text-muted">{editor.content.length}/2000</span>
                <button
                    className="h-9 bg-ink px-5 text-xs font-bold text-white disabled:opacity-50"
                    type="submit"
                    disabled={isSaving || !editor.content.trim()}
                >
                    {isSaving ? '저장 중...' : '저장'}
                </button>
            </div>
        </form>
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
