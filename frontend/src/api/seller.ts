import type {
    SellerOrder,
    SellerOrderDetail,
    SellerOrderItemFulfillmentUpdateRequest,
    SellerOrderPage,
    SellerProductDetail,
    SellerProductPage,
    SellerProductRequest,
    SellerProductStockCondition,
    SellerProfile,
    SellerProfileCreateRequest,
    SellerProfileUpdateRequest,
    SellerSettlementAccount,
    SellerSettlementAccountUpsertRequest,
    SettlementAvailability,
    SettlementRequest,
    SettlementRequestPage,
    SettlementRequestWorkType,
    FulfillmentStatus,
    SellerPendingOrderCount,
} from '../types/seller'
import { apiRequest } from './client'
import type {
    PaymentRefund,
    PaymentRefundRequest,
    ReturnRequest,
    ReturnRequestStatus,
} from '../types/order'
import type { PageResponse } from '../types/api'

export function getSellerProfile(signal?: AbortSignal) {
    return apiRequest<SellerProfile>('/seller/profile', { signal })
}

export function createSellerProfile(request: SellerProfileCreateRequest) {
    return apiRequest<SellerProfile>('/seller/profile', { method: 'POST', body: request })
}

export function updateSellerProfile(request: SellerProfileUpdateRequest) {
    return apiRequest<SellerProfile>('/seller/profile', { method: 'PUT', body: request })
}

export function getSellerSettlementAccount(signal?: AbortSignal) {
    return apiRequest<SellerSettlementAccount>('/seller/settlement-account', { signal })
}

export function upsertSellerSettlementAccount(
    request: SellerSettlementAccountUpsertRequest,
) {
    return apiRequest<SellerSettlementAccount>('/seller/settlement-account', {
        method: 'PUT',
        body: request,
    })
}

export function getSettlementAvailability(signal?: AbortSignal) {
    return apiRequest<SettlementAvailability>(
        '/seller/settlement-requests/availability',
        { signal },
    )
}

export function getSettlementRequests({
    page = 1,
    size = 20,
    status,
    workType,
    requestId,
    requestedFrom,
    requestedTo,
    signal,
}: {
    page?: number
    size?: number
    status?: SettlementRequest['status']
    workType?: SettlementRequestWorkType
    requestId?: number
    requestedFrom?: string
    requestedTo?: string
    signal?: AbortSignal
} = {}) {
    const query = new URLSearchParams({
        page: String(page),
        size: String(size),
    })
    if (status) query.set('status', status)
    if (workType) query.set('workType', workType)
    if (requestId !== undefined) query.set('requestId', String(requestId))
    if (requestedFrom) query.set('requestedFrom', requestedFrom)
    if (requestedTo) query.set('requestedTo', requestedTo)
    return apiRequest<SettlementRequestPage>(
        `/seller/settlement-requests?${query.toString()}`,
        { signal },
    )
}

export function getSettlementRequest(
    settlementRequestId: number,
    signal?: AbortSignal,
) {
    return apiRequest<SettlementRequest>(
        `/seller/settlement-requests/${settlementRequestId}`,
        { signal },
    )
}

export function createSettlementRequest() {
    return apiRequest<SettlementRequest>('/seller/settlement-requests', {
        method: 'POST',
        body: {},
    })
}

const SELLER_PAGE_SIZE = 20

interface SellerPageOptions {
    page?: number
    size?: number
    signal?: AbortSignal
}

interface SellerProductPageOptions extends SellerPageOptions {
    keyword?: string
    categoryId?: number
    stockCondition?: SellerProductStockCondition
    stockQuantity?: number
}

interface SellerOrderPageOptions extends SellerPageOptions {
    keyword?: string
    fulfillmentStatus?: FulfillmentStatus | ''
    workType?: SellerOrderWorkType
}

export type SellerOrderWorkType = 'ACTION_REQUIRED'

export function getSellerProducts(options: SellerProductPageOptions = {}) {
    const {
        page = 1,
        size = SELLER_PAGE_SIZE,
        keyword = '',
        categoryId,
        stockCondition,
        stockQuantity,
        signal,
    } = options
    const query = new URLSearchParams({
        page: String(page),
        size: String(size),
        keyword,
    })
    if (categoryId !== undefined) query.set('categoryId', String(categoryId))
    if (stockCondition) query.set('stockCondition', stockCondition)
    if (stockQuantity !== undefined) query.set('stockQuantity', String(stockQuantity))
    return apiRequest<SellerProductPage>(
        `/seller/products?${query.toString()}`,
        { signal },
    )
}

export function getSellerProduct(productId: number) {
    return apiRequest<SellerProductDetail>(`/seller/products/${productId}`)
}

export function createSellerProduct(request: SellerProductRequest) {
    return apiRequest<SellerProductDetail>('/seller/products', { method: 'POST', body: request })
}

export function updateSellerProduct(productId: number, request: SellerProductRequest) {
    return apiRequest<SellerProductDetail>(`/seller/products/${productId}`, {
        method: 'PUT',
        body: request,
    })
}

export function deleteSellerProduct(productId: number) {
    return apiRequest<void>(`/seller/products/${productId}`, { method: 'DELETE' })
}

export function getSellerOrders(options: SellerOrderPageOptions = {}) {
    const {
        page = 1,
        size = SELLER_PAGE_SIZE,
        keyword = '',
        signal,
        fulfillmentStatus,
        workType,
    } = options
    const query = new URLSearchParams({
        page: String(page),
        size: String(size),
        keyword,
    })
    if (fulfillmentStatus) query.set('fulfillmentStatus', fulfillmentStatus)
    if (workType) query.set('workType', workType)
    return apiRequest<SellerOrderPage>(
        `/seller/orders?${query.toString()}`,
        { signal },
    )
}

export const SELLER_PENDING_ORDER_COUNT_CHANGED_EVENT =
    'ymall:seller-pending-order-count-changed'

export function getSellerPendingOrderCount(signal?: AbortSignal) {
    return apiRequest<SellerPendingOrderCount>('/seller/orders/pending-count', { signal })
}

export function notifySellerPendingOrderCountChanged() {
    window.dispatchEvent(new Event(SELLER_PENDING_ORDER_COUNT_CHANGED_EVENT))
}

export function getSellerOrder(orderId: number, signal?: AbortSignal) {
    return apiRequest<SellerOrderDetail>(`/seller/orders/${orderId}`, { signal })
}

export function updateSellerOrderItemFulfillment(
    orderId: number,
    orderItemId: number,
    request: SellerOrderItemFulfillmentUpdateRequest,
) {
    return apiRequest<SellerOrderDetail>(
        `/seller/orders/${orderId}/items/${orderItemId}/fulfillment`,
        {
            method: 'PATCH',
            body: request,
        },
    ).then((response) => {
        notifySellerPendingOrderCountChanged()
        return response
    })
}

export function updateSellerOrderStatus(orderId: number, fulfillmentStatus: FulfillmentStatus) {
    return apiRequest<SellerOrder>(`/seller/orders/${orderId}/status`, {
        method: 'PATCH',
        body: { fulfillmentStatus },
    }).then((response) => {
        notifySellerPendingOrderCountChanged()
        return response
    })
}

export function requestSellerRefund(orderId: number, request: PaymentRefundRequest) {
    return apiRequest<PaymentRefund>(`/seller/orders/${orderId}/refunds`, {
        method: 'POST',
        body: request,
    }).then((response) => {
        notifySellerPendingOrderCountChanged()
        return response
    })
}

export function getSellerRefunds(orderId: number, signal?: AbortSignal) {
    return apiRequest<PaymentRefund[]>(`/seller/orders/${orderId}/refunds`, { signal })
}

interface SellerReturnRequestOptions {
    page?: number
    size?: number
    status?: ReturnRequestStatus | ''
    keyword?: string
    signal?: AbortSignal
}

export function getSellerReturnRequests(options: SellerReturnRequestOptions = {}) {
    const {
        page = 1,
        size = SELLER_PAGE_SIZE,
        status,
        keyword = '',
        signal,
    } = options
    const query = new URLSearchParams({
        page: String(page),
        size: String(size),
        keyword,
    })
    if (status) query.set('status', status)
    return apiRequest<PageResponse<ReturnRequest>>(
        `/seller/return-requests?${query.toString()}`,
        { signal },
    )
}

export function getSellerReturnRequest(
    returnRequestId: number,
    signal?: AbortSignal,
) {
    return apiRequest<ReturnRequest>(
        `/seller/return-requests/${returnRequestId}`,
        { signal },
    )
}

export function approveSellerReturnRequest(
    returnRequestId: number,
    response: string,
) {
    return apiRequest<ReturnRequest>(
        `/seller/return-requests/${returnRequestId}/approval`,
        { method: 'PATCH', body: { response } },
    )
}

export function rejectSellerReturnRequest(
    returnRequestId: number,
    response: string,
) {
    return apiRequest<ReturnRequest>(
        `/seller/return-requests/${returnRequestId}/rejection`,
        { method: 'PATCH', body: { response } },
    )
}
