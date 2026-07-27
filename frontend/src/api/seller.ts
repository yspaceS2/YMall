import type {
    SellerOrder,
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
import type { PaymentRefund, PaymentRefundRequest } from '../types/order'

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

export function getSellerOrders(options: SellerPageOptions = {}) {
    const { page = 1, signal } = options
    return apiRequest<SellerOrderPage>(
        `/seller/orders?page=${page}&size=${SELLER_PAGE_SIZE}`,
        { signal },
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
