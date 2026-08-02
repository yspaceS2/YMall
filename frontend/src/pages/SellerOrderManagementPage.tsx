import {
    ArrowLeft,
    LoaderCircle,
    PackageCheck,
    RotateCcw,
    Truck,
} from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import {
    getSellerOrder,
    getSellerOrders,
    getSellerRefunds,
    requestSellerRefund,
    updateSellerOrderItemFulfillment,
    type SellerOrderWorkType,
} from '../api/seller'
import { RefundDialog } from '../components/RefundDialog'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import {
    ManagementListSearch,
    ManagementPagination,
} from '../components/management/ManagementListUi'
import type { PaymentRefund, PaymentRefundRequest } from '../types/order'
import type {
    FulfillmentStatus,
    SellerOrder,
    SellerOrderDetail,
    SellerOrderItem,
} from '../types/seller'
import { formatKoreanDateTime } from '../utils/dateTime'
import { formatPrice } from '../utils/product'

const statusLabels: Record<FulfillmentStatus, string> = {
    PENDING: '처리 대기',
    PREPARING: '상품 준비 중',
    SHIPPED: '배송 중',
    DELIVERED: '배송 완료',
}

type OrderFilter = FulfillmentStatus | SellerOrderWorkType | ''

const statusOptions: Array<{ value: OrderFilter; label: string }> = [
    { value: '', label: '전체 상태' },
    { value: 'ACTION_REQUIRED', label: '처리 필요' },
    { value: 'PENDING', label: statusLabels.PENDING },
    { value: 'PREPARING', label: statusLabels.PREPARING },
    { value: 'SHIPPED', label: statusLabels.SHIPPED },
    { value: 'DELIVERED', label: statusLabels.DELIVERED },
]

export function SellerOrderListPage() {
    const navigate = useNavigate()
    const [searchParams, setSearchParams] = useSearchParams()
    const [orders, setOrders] = useState<SellerOrder[]>([])
    const [totalPages, setTotalPages] = useState(0)
    const [totalElements, setTotalElements] = useState(0)
    const [loadedQueryKey, setLoadedQueryKey] = useState('')
    const [errorMessage, setErrorMessage] = useState('')
    const page = positivePage(searchParams.get('page'))
    const keyword = searchParams.get('keyword') ?? ''
    const workType = parseWorkType(searchParams.get('workType'))
    const status = workType ? '' : parseFulfillmentStatus(searchParams.get('fulfillmentStatus'))
    const selectedFilter: OrderFilter = workType ?? status
    const queryKey = `${page}:${keyword}:${selectedFilter}`
    const isLoading = loadedQueryKey !== queryKey

    useEffect(() => {
        const controller = new AbortController()
        getSellerOrders({
            page,
            keyword,
            fulfillmentStatus: status,
            workType,
            signal: controller.signal,
        }).then((response) => {
            setOrders(response.content)
            setTotalPages(response.totalPages)
            setTotalElements(response.totalElements)
            setErrorMessage('')
        }).catch((error: unknown) => {
            if (error instanceof Error && error.name === 'AbortError') return
            setErrorMessage(error instanceof ApiError
                ? error.message
                : '주문 목록을 불러오지 못했습니다.')
        }).finally(() => {
            if (!controller.signal.aborted) setLoadedQueryKey(queryKey)
        })
        return () => controller.abort()
    }, [keyword, page, queryKey, status, workType])

    return (
        <ManagementPage
            eyebrow="ORDER FULFILLMENT"
            title="주문·배송 관리"
            description={`주문 상품별로 출고 상태와 운송장 정보를 관리합니다. 총 ${totalElements.toLocaleString()}건`}
        >
            <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
                <label className="grid gap-2 text-xs font-bold">
                    배송 상태
                    <select
                        className="h-11 min-w-44 border border-line bg-surface px-3 text-sm"
                        value={selectedFilter}
                        onChange={(event) => {
                            const next = new URLSearchParams(searchParams)
                            next.delete('fulfillmentStatus')
                            next.delete('workType')
                            if (event.target.value === 'ACTION_REQUIRED') {
                                next.set('workType', event.target.value)
                            } else if (event.target.value) {
                                next.set('fulfillmentStatus', event.target.value)
                            }
                            next.set('page', '1')
                            setSearchParams(next)
                        }}
                    >
                        {statusOptions.map((option) => (
                            <option key={option.value || 'all'} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </select>
                </label>
                <p className="text-xs text-muted">
                    고객 배송 정보는 주문 상세에서만 확인할 수 있습니다.
                </p>
            </div>
            <ManagementListSearch
                key={keyword}
                placeholder="주문번호 또는 상품명을 검색하세요"
            />

            {errorMessage && (
                <FeedbackMessage className="mb-5" tone="error">{errorMessage}</FeedbackMessage>
            )}
            {isLoading ? (
                <div className="grid min-h-70 place-content-center">
                    <LoaderCircle className="size-6 animate-spin" />
                </div>
            ) : orders.length === 0 ? (
                <div className="border border-line bg-surface px-6 py-16 text-center text-sm text-muted">
                    조건에 맞는 주문이 없습니다.
                </div>
            ) : (
                <div className="overflow-x-auto border-y border-line">
                    <table className="w-full min-w-210 border-collapse text-left">
                        <thead className="bg-paper text-xs text-muted">
                            <tr>
                                <th className="px-4 py-3">주문번호</th>
                                <th className="px-4 py-3">상품</th>
                                <th className="px-4 py-3">판매금액</th>
                                <th className="px-4 py-3">상태</th>
                                <th className="px-4 py-3">주문일</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-line">
                            {orders.map((order) => {
                                const firstItem = order.items[0]
                                const activeStatuses = Array.from(new Set(
                                    order.items
                                        .filter((item) => item.quantity > item.refundedQuantity)
                                        .map((item) => item.fulfillmentStatus),
                                ))
                                return (
                                    <tr
                                        className="cursor-pointer bg-surface transition-colors hover:bg-paper"
                                        key={order.orderId}
                                        tabIndex={0}
                                        onClick={() => navigate(`/seller/orders/${order.orderId}`)}
                                        onKeyDown={(event) => {
                                            if (event.key === 'Enter' || event.key === ' ') {
                                                event.preventDefault()
                                                navigate(`/seller/orders/${order.orderId}`)
                                            }
                                        }}
                                    >
                                        <td className="px-4 py-4 text-sm font-bold">#{order.orderId}</td>
                                        <td className="px-4 py-4">
                                            <div className="flex items-center gap-3">
                                                <ProductThumbnail item={firstItem} />
                                                <div>
                                                    <strong className="block max-w-80 truncate text-sm">
                                                        {firstItem?.productName ?? '상품 정보 없음'}
                                                        {order.items.length > 1 && ` 외 ${order.items.length - 1}개`}
                                                    </strong>
                                                    <span className="mt-1 block text-xs text-muted">
                                                        총 {order.items.length}개 품목
                                                    </span>
                                                </div>
                                            </div>
                                        </td>
                                        <td className="px-4 py-4 text-sm">{formatPrice(order.sellerAmount)}</td>
                                        <td className="px-4 py-4">
                                            {activeStatuses.length === 0 ? (
                                                <span className="text-xs font-bold text-muted">
                                                    판매 취소
                                                </span>
                                            ) : (
                                                <div className="flex flex-wrap gap-1.5">
                                                    {activeStatuses.map((itemStatus) => (
                                                        <StatusBadge
                                                            key={itemStatus}
                                                            status={itemStatus}
                                                        />
                                                    ))}
                                                </div>
                                            )}
                                        </td>
                                        <td className="px-4 py-4 text-xs text-muted">
                                            {formatKoreanDateTime(order.createdAt)}
                                        </td>
                                    </tr>
                                )
                            })}
                        </tbody>
                    </table>
                </div>
            )}

            <ManagementPagination page={page} totalPages={totalPages} />
        </ManagementPage>
    )
}

function positivePage(value: string | null) {
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed > 0 ? parsed : 1
}

function parseFulfillmentStatus(value: string | null): FulfillmentStatus | '' {
    return value === 'PENDING'
        || value === 'PREPARING'
        || value === 'SHIPPED'
        || value === 'DELIVERED'
        ? value
        : ''
}

function parseWorkType(value: string | null): SellerOrderWorkType | undefined {
    return value === 'ACTION_REQUIRED' ? value : undefined
}

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
                                            <StatusBadge status={item.fulfillmentStatus} />
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

function ManagementPage({
    eyebrow,
    title,
    description,
    children,
}: {
    eyebrow: string
    title: string
    description?: string
    children: React.ReactNode
}) {
    return (
        <section className="mx-auto max-w-350 px-4 py-10 min-[601px]:px-8 min-[601px]:py-14">
            <p className="text-[10px] font-extrabold tracking-[.18em] text-accent">{eyebrow}</p>
            <div className="mb-8 mt-2">
                <h1 className="font-serif text-[clamp(34px,5vw,54px)] leading-none tracking-tighter">{title}</h1>
                {description && <p className="mt-3 text-sm text-muted">{description}</p>}
            </div>
            {children}
        </section>
    )
}

function ProductThumbnail({ item, large = false }: { item?: SellerOrderItem; large?: boolean }) {
    const size = large ? 'size-22' : 'size-14'
    return item?.thumbnailUrl ? (
        <img
            className={`${size} shrink-0 object-cover`}
            src={item.thumbnailUrl}
            alt=""
        />
    ) : (
        <span className={`${size} grid shrink-0 place-items-center bg-paper text-muted`}>
            <PackageCheck className="size-5" />
        </span>
    )
}

function StatusBadge({ status }: { status: FulfillmentStatus }) {
    const colors = status === 'DELIVERED'
        ? 'bg-success-soft text-success'
        : status === 'SHIPPED'
            ? 'bg-info-soft text-info'
            : status === 'PREPARING'
                ? 'bg-warning-soft text-warning'
                : 'bg-paper text-muted'
    return (
        <span className={`inline-flex px-2.5 py-1 text-[10px] font-extrabold ${colors}`}>
            {statusLabels[status]}
        </span>
    )
}

function InfoTerm({ label, value }: { label: string; value: string }) {
    return (
        <div className="grid gap-1 min-[501px]:grid-cols-[80px_1fr]">
            <dt className="text-xs font-bold text-muted">{label}</dt>
            <dd>{value}</dd>
        </div>
    )
}

function ActionButton({
    label,
    danger = false,
    disabled,
    onClick,
}: {
    label: string
    danger?: boolean
    disabled: boolean
    onClick: () => void
}) {
    return (
        <button
            className={[
                'h-10 border px-4 text-xs font-bold disabled:opacity-40',
                danger
                    ? 'border-danger text-danger'
                    : 'border-ink text-ink',
            ].join(' ')}
            type="button"
            disabled={disabled}
            onClick={onClick}
        >
            {danger ? <RotateCcw className="mr-1.5 inline size-3.5" /> : null}
            {label}
        </button>
    )
}
