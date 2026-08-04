import { LoaderCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { getApiErrorMessage, isAbortError } from '../api/errors'
import { getSellerOrders, type SellerOrderWorkType } from '../api/seller'
import {
    ManagementListSearch,
    ManagementPagination,
} from '../components/management/ManagementListUi'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import type { FulfillmentStatus, SellerOrder } from '../types/seller'
import { formatKoreanDateTime } from '../utils/dateTime'
import { formatPrice } from '../utils/product'
import { parsePositiveInteger } from '../utils/searchParams'
import {
    ManagementPage,
    ProductThumbnail,
    FulfillmentStatusBadge,
} from './SellerOrderPageUi'
import { statusLabels } from './sellerOrderStatus'

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
    const page = parsePositiveInteger(searchParams.get('page'), 1)
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
            if (isAbortError(error)) return
            setErrorMessage(getApiErrorMessage(error, '주문 목록을 불러오지 못했습니다.'))
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
                                                        <FulfillmentStatusBadge
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
