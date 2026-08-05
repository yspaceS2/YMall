import { ArrowLeft } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { RefundDialog } from '../components/RefundDialog'
import { ReturnRequestDialog } from '../components/ReturnRequestDialog'
import { OrderItemsSection } from '../components/order/OrderItemsSection'
import { OrderStatusBadge } from '../components/order/OrderStatusBadge'
import { OrderSummarySidebar } from '../components/order/OrderSummarySidebar'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { PageState } from '../components/ui/PageState'
import { useOrderDetail } from '../hooks/useOrderDetail'
import { formatOrderDate } from '../utils/order'

export function OrderDetailPage() {
    const { orderId: orderIdParam } = useParams()
    const orderId = Number(orderIdParam)
    const isValidOrderId = Number.isInteger(orderId) && orderId > 0
    const detail = useOrderDetail(orderId, isValidOrderId)

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

    if (detail.isLoading) {
        return <PageState variant="loading" title="주문 상세를 불러오는 중입니다" description="잠시만 기다려 주세요." />
    }

    if (!detail.order) {
        return (
            <PageState
                variant="error"
                title="주문 상세를 불러오지 못했습니다"
                description={detail.errorMessage}
                action={(
                    <button
                        className="border border-ink bg-surface px-5 py-2.5 text-xs font-bold"
                        type="button"
                        onClick={detail.retry}
                    >
                        다시 시도
                    </button>
                )}
            />
        )
    }

    const order = detail.order
    const representativeItem = order.items[0]
    const additionalItemCount = Math.max(order.items.length - 1, 0)
    const orderTitle = representativeItem
        ? `${representativeItem.productName}${additionalItemCount > 0 ? ` 외 ${additionalItemCount}개` : ''}`
        : `주문 #${order.orderId}`

    return (
        <section className="mx-auto max-w-260 px-4 py-12 min-[601px]:px-8 min-[601px]:py-16">
            <Link className="mb-8 inline-flex items-center gap-2 text-xs font-bold text-muted hover:text-ink" to="/mypage/orders">
                <ArrowLeft className="size-4" />
                주문 내역으로
            </Link>

            <header className="border-b border-ink pb-8">
                <div className="flex flex-wrap items-center gap-3">
                    <p className="text-[11px] font-extrabold tracking-[.18em] text-accent">ORDER DETAIL</p>
                    <OrderStatusBadge status={order.status} />
                </div>
                <h1 className="mt-3 font-serif text-[clamp(34px,5vw,56px)] leading-tight tracking-tight">{orderTitle}</h1>
                <p className="mt-4 text-xs text-muted">
                    주문 #{order.orderId} · {formatOrderDate(order.createdAt)}
                </p>
            </header>

            {detail.errorMessage && (
                <FeedbackMessage className="mt-6" tone="error">
                    {detail.errorMessage}
                </FeedbackMessage>
            )}

            <div className="grid gap-10 py-10 min-[901px]:grid-cols-[minmax(0,1fr)_300px]">
                <OrderItemsSection
                    editor={detail.editor}
                    isSavingReview={detail.isSavingReview}
                    order={order}
                    returnRequests={detail.returnRequests}
                    reviewsByOrderItemId={detail.reviewsByOrderItemId}
                    onCancelEditor={detail.closeEditor}
                    onChangeEditor={detail.updateEditor}
                    onDeleteReview={detail.setReviewToDelete}
                    onEditReview={detail.openReviewEditor}
                    onRequestReturn={detail.openReturnDialog}
                    onSaveReview={detail.saveReview}
                />
                <OrderSummarySidebar
                    order={order}
                    onOpenRefund={() => void detail.openRefundDialog()}
                />
            </div>

            <ConfirmDialog
                open={detail.reviewToDelete !== null}
                title="리뷰를 삭제할까요?"
                description="삭제한 리뷰는 복구할 수 없습니다."
                confirmLabel="리뷰 삭제"
                isPending={detail.isSavingReview}
                onCancel={() => detail.setReviewToDelete(null)}
                onConfirm={() => void detail.removeSelectedReview()}
            />
            <RefundDialog
                key={detail.refundDialogOpen ? `refund-${order.orderId}` : 'refund-closed'}
                open={detail.refundDialogOpen}
                orderId={order.orderId}
                items={order.items}
                refunds={detail.refunds}
                isLoadingHistory={detail.isLoadingRefunds}
                isSubmitting={detail.isRefunding}
                errorMessage={detail.refundError}
                onClose={detail.closeRefundDialog}
                onSubmit={detail.submitRefund}
            />
            <ReturnRequestDialog
                key={detail.returnItem ? `return-${detail.returnItem.orderItemId}` : 'return-closed'}
                open={detail.returnItem !== null}
                item={detail.returnItem}
                isSubmitting={detail.isRequestingReturn}
                errorMessage={detail.returnError}
                onClose={detail.closeReturnDialog}
                onSubmit={detail.submitReturnRequest}
            />
        </section>
    )
}
