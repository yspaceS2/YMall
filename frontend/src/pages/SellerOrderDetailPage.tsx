import {
    ArrowLeft,
    LoaderCircle,
    Truck,
} from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import {
    getSellerOrder,
    getSellerRefunds,
    requestSellerRefund,
    updateSellerOrderItemFulfillment,
} from '../api/seller'
import { RefundDialog } from '../components/RefundDialog'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import type { PaymentRefund, PaymentRefundRequest } from '../types/order'
import type {
    FulfillmentStatus,
    SellerOrderDetail,
    SellerOrderItem,
} from '../types/seller'
import { formatKoreanDateTime } from '../utils/dateTime'
import { formatPrice } from '../utils/product'
import {
    ActionButton,
    InfoTerm,
    ManagementPage,
    ProductThumbnail,
    FulfillmentStatusBadge,
} from './SellerOrderPageUi'
import { statusLabels } from './sellerOrderStatus'

export function SellerOrderDetailPage() {
    const { orderId: orderIdParam } = useParams()
    const orderId = Number(orderIdParam)
    const isValidOrderId = Number.isInteger(orderId) && orderId > 0
    const [order, setOrder] = useState<SellerOrderDetail | null>(null)
    const [shippingItem, setShippingItem] = useState<SellerOrderItem | null>(null)
    const [carrier, setCarrier] = useState('')
    const [trackingNumber, setTrackingNumber] = useState('')
    const [isLoading, setIsLoading] = useState(isValidOrderId)
    const [isSaving, setIsSaving] = useState(false)
    const [message, setMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')
    const [refundItem, setRefundItem] = useState<SellerOrderItem | null>(null)
    const [refunds, setRefunds] = useState<PaymentRefund[]>([])
    const [isLoadingRefunds, setIsLoadingRefunds] = useState(false)
    const [isRefunding, setIsRefunding] = useState(false)
    const [refundError, setRefundError] = useState('')

    useEffect(() => {
        if (!isValidOrderId) return
        const controller = new AbortController()
        getSellerOrder(orderId, controller.signal).then(setOrder).catch((error: unknown) => {
            if (error instanceof Error && error.name === 'AbortError') return
            setErrorMessage(error instanceof ApiError
                ? error.message
                : '주문 상세를 불러오지 못했습니다.')
        }).finally(() => {
            if (!controller.signal.aborted) setIsLoading(false)
        })
        return () => controller.abort()
    }, [isValidOrderId, orderId])

    async function updateItem(
        item: SellerOrderItem,
        fulfillmentStatus: FulfillmentStatus,
        shipping?: { carrier: string; trackingNumber: string },
    ) {
        if (!order) return
        setIsSaving(true)
        setErrorMessage('')
        try {
            const updated = await updateSellerOrderItemFulfillment(
                order.orderId,
                item.orderItemId,
                {
                    fulfillmentStatus,
                    carrier: shipping?.carrier,
                    trackingNumber: shipping?.trackingNumber,
                },
            )
            setOrder(updated)
            setShippingItem(null)
            setCarrier('')
            setTrackingNumber('')
            setMessage(`${item.productName}의 상태를 ${statusLabels[fulfillmentStatus]}으로 변경했습니다.`)
        } catch (error) {
            setErrorMessage(error instanceof ApiError
                ? error.message
                : '배송 상태를 변경하지 못했습니다.')
        } finally {
            setIsSaving(false)
        }
    }

    function submitShipping(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        if (!shippingItem || !carrier.trim() || !trackingNumber.trim()) return
        void updateItem(shippingItem, 'SHIPPED', {
            carrier: carrier.trim(),
            trackingNumber: trackingNumber.trim(),
        })
    }

    async function openSellerCancel(item: SellerOrderItem) {
        if (!order) return
        setRefundItem(item)
        setRefunds([])
        setRefundError('')
        setIsLoadingRefunds(true)
        try {
            setRefunds(await getSellerRefunds(order.orderId))
        } catch (error) {
            setRefundError(error instanceof ApiError
                ? error.message
                : '환불 내역을 불러오지 못했습니다.')
        } finally {
            setIsLoadingRefunds(false)
        }
    }

    async function submitSellerCancel(request: PaymentRefundRequest) {
        if (!order || !refundItem) return false
        const selected = request.items?.find(
            (item) => item.orderItemId === refundItem.orderItemId,
        )
        if (!selected) return false
        setRefundError('')
        setIsRefunding(true)
        try {
            await requestSellerRefund(order.orderId, {
                ...request,
                items: [selected],
            })
            const [updated, refundHistory] = await Promise.all([
                getSellerOrder(order.orderId),
                getSellerRefunds(order.orderId),
            ])
            setOrder(updated)
            setRefunds(refundHistory)
            setMessage(`${refundItem.productName}의 판매 취소 및 환불을 처리했습니다.`)
            return true
        } catch (error) {
            setRefundError(error instanceof ApiError
                ? error.message
                : '판매 취소를 처리하지 못했습니다.')
            return false
        } finally {
            setIsRefunding(false)
        }
    }

    if (isLoading) {
        return <div className="grid min-h-100 place-content-center"><LoaderCircle className="size-6 animate-spin" /></div>
    }

    if (!order) {
        return (
            <ManagementPage eyebrow="ORDER DETAIL" title="주문 상세">
                <FeedbackMessage tone="error">
                    {!isValidOrderId
                        ? '올바르지 않은 주문 번호입니다.'
                        : errorMessage || '주문을 찾을 수 없습니다.'}
                </FeedbackMessage>
            </ManagementPage>
        )
    }

    const address = order.deliveryAddress
    return (
        <ManagementPage
            eyebrow={`ORDER #${order.orderId}`}
            title="주문 상세"
            description={`주문일 ${formatKoreanDateTime(order.createdAt)}`}
        >
            <Link className="mb-6 inline-flex items-center gap-2 text-xs font-bold" to="/seller/orders">
                <ArrowLeft className="size-4" /> 주문 목록
            </Link>
            {message && <FeedbackMessage className="mb-5" tone="success">{message}</FeedbackMessage>}
            {errorMessage && <FeedbackMessage className="mb-5" tone="error">{errorMessage}</FeedbackMessage>}

            <section className="mb-8 border-t-2 border-ink">
                <h2 className="border-b border-line py-4 text-base font-bold">배송지 정보</h2>
                {address?.masked ? (
                    <p className="border-b border-line bg-surface px-5 py-5 text-sm text-muted">
                        배송 완료 후 보존 기간이 지나 개인정보가 마스킹되었습니다.
                    </p>
                ) : address ? (
                    <dl className="grid gap-x-8 gap-y-4 border-b border-line bg-surface px-5 py-5 text-sm min-[701px]:grid-cols-2">
                        <InfoTerm label="받는 분" value={address.recipientName} />
                        <InfoTerm label="연락처" value={address.recipientPhone} />
                        <div className="min-[701px]:col-span-2">
                            <InfoTerm
                                label="주소"
                                value={`(${address.postalCode}) ${address.roadAddress}${address.detailAddress ? ` ${address.detailAddress}` : ''}`}
                            />
                        </div>
                    </dl>
                ) : (
                    <p className="border-b border-line bg-surface px-5 py-5 text-sm text-muted">
                        저장된 배송지 정보가 없습니다.
                    </p>
                )}
            </section>

            <section className="border-t-2 border-ink">
                <div className="flex items-center justify-between border-b border-line py-4">
                    <h2 className="text-base font-bold">주문 상품</h2>
                    <strong>{formatPrice(order.sellerAmount)}</strong>
                </div>
                <div className="divide-y divide-line">
                    {order.items.map((item) => {
                        const remaining = item.quantity - item.refundedQuantity
                        return (
                            <article className="grid gap-5 py-6 min-[801px]:grid-cols-[1fr_auto]" key={item.orderItemId}>
                                <div className="flex gap-4">
                                    <ProductThumbnail item={item} large />
                                    <div>
                                        <strong className="text-sm">{item.productName}</strong>
                                        <p className="mt-2 text-xs text-muted">
                                            {formatPrice(item.unitPrice)} · 주문 {item.quantity}개
                                            {item.refundedQuantity > 0 && ` · 취소 ${item.refundedQuantity}개`}
                                        </p>
                                        <div className="mt-3 flex flex-wrap items-center gap-2">
                                            <FulfillmentStatusBadge status={item.fulfillmentStatus} />
                                            {item.carrier && item.trackingNumber && (
                                                <span className="text-xs text-muted">
                                                    {item.carrier} {item.trackingNumber}
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                </div>
                                <div className="flex flex-wrap items-center gap-2 min-[801px]:justify-end">
                                    {remaining > 0 && item.fulfillmentStatus === 'PENDING' && (
                                        <>
                                            <ActionButton
                                                label="상품 준비"
                                                disabled={isSaving}
                                                onClick={() => void updateItem(item, 'PREPARING')}
                                            />
                                            {order.refundSupported && (
                                                <ActionButton
                                                    label="판매 취소"
                                                    danger
                                                    disabled={isSaving}
                                                    onClick={() => void openSellerCancel(item)}
                                                />
                                            )}
                                        </>
                                    )}
                                    {remaining > 0 && item.fulfillmentStatus === 'PREPARING' && (
                                        <ActionButton
                                            label="운송장 등록"
                                            disabled={isSaving}
                                            onClick={() => {
                                                setShippingItem(item)
                                                setCarrier(item.carrier ?? '')
                                                setTrackingNumber(item.trackingNumber ?? '')
                                            }}
                                        />
                                    )}
                                    {remaining > 0 && item.fulfillmentStatus === 'SHIPPED' && (
                                        <ActionButton
                                            label="배송 완료"
                                            disabled={isSaving}
                                            onClick={() => void updateItem(item, 'DELIVERED')}
                                        />
                                    )}
                                </div>
                            </article>
                        )
                    })}
                </div>
            </section>

            {shippingItem && (
                <div className="fixed inset-0 z-50 grid place-items-center bg-black/45 p-4">
                    <form className="w-full max-w-120 bg-surface p-6 shadow-2xl" onSubmit={submitShipping}>
                        <div className="flex items-center gap-2">
                            <Truck className="size-5" />
                            <h2 className="text-lg font-bold">운송장 등록</h2>
                        </div>
                        <p className="mt-2 text-sm text-muted">{shippingItem.productName}</p>
                        <label className="mt-5 grid gap-2 text-xs font-bold">
                            택배사
                            <input
                                className="h-11 border border-line bg-paper px-3 text-sm font-normal"
                                value={carrier}
                                maxLength={50}
                                onChange={(event) => setCarrier(event.target.value)}
                                placeholder="예: CJ대한통운"
                                required
                            />
                        </label>
                        <label className="mt-4 grid gap-2 text-xs font-bold">
                            운송장 번호
                            <input
                                className="h-11 border border-line bg-paper px-3 text-sm font-normal"
                                value={trackingNumber}
                                maxLength={100}
                                onChange={(event) => setTrackingNumber(event.target.value)}
                                placeholder="숫자와 하이픈을 포함해 입력"
                                required
                            />
                        </label>
                        <div className="mt-6 flex justify-end gap-2">
                            <button
                                className="h-11 border border-line px-5 text-xs font-bold"
                                type="button"
                                disabled={isSaving}
                                onClick={() => setShippingItem(null)}
                            >
                                취소
                            </button>
                            <button
                                className="h-11 bg-ink px-5 text-xs font-bold text-white disabled:opacity-50"
                                type="submit"
                                disabled={isSaving || !carrier.trim() || !trackingNumber.trim()}
                            >
                                {isSaving ? '등록 중...' : '배송 시작'}
                            </button>
                        </div>
                    </form>
                </div>
            )}

            <RefundDialog
                key={refundItem?.orderItemId ?? 'closed'}
                open={refundItem !== null}
                orderId={order.orderId}
                items={refundItem ? [refundItem] : []}
                refunds={refunds}
                isLoadingHistory={isLoadingRefunds}
                isSubmitting={isRefunding}
                errorMessage={refundError}
                mode="sellerCancel"
                onClose={() => {
                    if (!isRefunding) setRefundItem(null)
                }}
                onSubmit={submitSellerCancel}
            />
        </ManagementPage>
    )
}
