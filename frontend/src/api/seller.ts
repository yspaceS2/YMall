import type {
    SellerOrder,
    SellerOrderDetail,
    SellerOrderItemFulfillmentUpdateRequest,
    SellerOrderPage,
    SellerProductDetail,
    SellerProductPage,
    SellerProductRequest,
    SellerProfile,
    SellerProfileCreateRequest,
    SellerProfileUpdateRequest,
    SellerSettlementAccount,
    SellerSettlementAccountUpsertRequest,
    SettlementAvailability,
    SettlementRequest,
    SettlementRequestPage,
    FulfillmentStatus,
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

export function getSettlementAvailability(period: string, signal?: AbortSignal) {
    return apiRequest<SettlementAvailability>(
        `/seller/settlement-requests/availability?period=${encodeURIComponent(period)}`,
        { signal },
    )
}

export function getSettlementRequests(signal?: AbortSignal) {
    return apiRequest<SettlementRequestPage>(
        '/seller/settlement-requests?page=1&size=24',
        { signal },
    )
}

export function createSettlementRequest(period: string) {
    return apiRequest<SettlementRequest>('/seller/settlement-requests', {
        method: 'POST',
        body: { period },
    })
}

const SELLER_PAGE_SIZE = 20

interface SellerPageOptions {
    page?: number
    signal?: AbortSignal
}

interface SellerOrderPageOptions extends SellerPageOptions {
    fulfillmentStatus?: FulfillmentStatus | ''
}

export function getSellerProducts(options: SellerPageOptions = {}) {
    const { page = 1, signal } = options
    return apiRequest<SellerProductPage>(
        `/seller/products?page=${page}&size=${SELLER_PAGE_SIZE}`,
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
    const { page = 1, signal, fulfillmentStatus } = options
    const query = new URLSearchParams({
        page: String(page),
        size: String(SELLER_PAGE_SIZE),
    })
    if (fulfillmentStatus) query.set('fulfillmentStatus', fulfillmentStatus)
    return apiRequest<SellerOrderPage>(
        `/seller/orders?${query.toString()}`,
        { signal },
    )
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
    )
}

export function updateSellerOrderStatus(orderId: number, fulfillmentStatus: FulfillmentStatus) {
    return apiRequest<SellerOrder>(`/seller/orders/${orderId}/status`, {
        method: 'PATCH',
        body: { fulfillmentStatus },
    })
}

export function requestSellerRefund(orderId: number, request: PaymentRefundRequest) {
    return apiRequest<PaymentRefund>(`/seller/orders/${orderId}/refunds`, {
        method: 'POST',
        body: request,
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
