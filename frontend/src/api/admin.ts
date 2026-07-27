import type {
    AdminMemberPage,
    AdminOrderPage,
    AdminProduct,
    AdminProductPage,
    AdminSellerPage,
    AdminSettlementRequest,
    AdminSettlementRequestPage,
    SettlementRequestStatus,
} from '../types/admin'
import { apiRequest } from './client'
import type { PaymentRefund, PaymentRefundRequest } from '../types/order'

const ADMIN_PAGE_SIZE = 20

interface AdminPageOptions {
    page?: number
    signal?: AbortSignal
}

export function getPendingProducts(options: AdminPageOptions = {}) {
    const { page = 1, signal } = options
    return apiRequest<AdminProductPage>(
        `/admin/products?status=PENDING&page=${page}&size=${ADMIN_PAGE_SIZE}`,
        { signal },
    )
}

export function updateAdminProductStatus(
    productId: number,
    status: 'APPROVED' | 'REJECTED',
) {
    return apiRequest<AdminProduct>(`/admin/products/${productId}/status`, {
        method: 'PATCH',
        body: { status },
    })
}

export function getAdminMembers(options: AdminPageOptions = {}) {
    const { page = 1, signal } = options
    return apiRequest<AdminMemberPage>(`/admin/members?page=${page}&size=${ADMIN_PAGE_SIZE}`, { signal })
}

export function getAdminSellers(options: AdminPageOptions = {}) {
    const { page = 1, signal } = options
    return apiRequest<AdminSellerPage>(`/admin/sellers?page=${page}&size=${ADMIN_PAGE_SIZE}`, { signal })
}

export function getAdminOrders(options: AdminPageOptions = {}) {
    const { page = 1, signal } = options
    return apiRequest<AdminOrderPage>(`/admin/orders?page=${page}&size=${ADMIN_PAGE_SIZE}`, { signal })
}

export function requestAdminRefund(orderId: number, request: PaymentRefundRequest) {
    return apiRequest<PaymentRefund>(`/admin/orders/${orderId}/refunds`, {
        method: 'POST',
        body: request,
    })
}

export function getAdminRefunds(orderId: number, signal?: AbortSignal) {
    return apiRequest<PaymentRefund[]>(`/admin/orders/${orderId}/refunds`, { signal })
}

export function getAdminSettlementRequests(
    status?: SettlementRequestStatus,
    signal?: AbortSignal,
) {
    const query = status ? `?status=${status}&page=1&size=50` : '?page=1&size=50'
    return apiRequest<AdminSettlementRequestPage>(
        `/admin/settlement-requests${query}`,
        { signal },
    )
}

export function approveAdminSettlementRequest(settlementRequestId: number) {
    return apiRequest<AdminSettlementRequest>(
        `/admin/settlement-requests/${settlementRequestId}/approval`,
        { method: 'PATCH' },
    )
}

export function rejectAdminSettlementRequest(
    settlementRequestId: number,
    reason: string,
) {
    return apiRequest<AdminSettlementRequest>(
        `/admin/settlement-requests/${settlementRequestId}/rejection`,
        { method: 'PATCH', body: { reason } },
    )
}

export function completeAdminMockSettlementPayment(settlementRequestId: number) {
    return apiRequest<AdminSettlementRequest>(
        `/admin/settlement-requests/${settlementRequestId}/mock-payments`,
        { method: 'POST' },
    )
}
