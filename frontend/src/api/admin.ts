import type {
    AdminCategory,
    AdminCategoryRequest,
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
import type { ProductStatus } from '../types/product'

const ADMIN_PAGE_SIZE = 20

export function getAdminCategories(keyword = '', signal?: AbortSignal) {
    const query = new URLSearchParams({ keyword })
    return apiRequest<AdminCategory[]>(`/admin/categories?${query.toString()}`, { signal })
}

export function createAdminCategory(request: AdminCategoryRequest) {
    return apiRequest<AdminCategory>('/admin/categories', {
        method: 'POST',
        body: request,
    })
}

export function updateAdminCategory(categoryId: number, request: AdminCategoryRequest) {
    return apiRequest<AdminCategory>(`/admin/categories/${categoryId}`, {
        method: 'PUT',
        body: request,
    })
}

export function deleteAdminCategory(categoryId: number) {
    return apiRequest<void>(`/admin/categories/${categoryId}`, { method: 'DELETE' })
}

interface AdminPageOptions {
    page?: number
    signal?: AbortSignal
}

interface AdminProductPageOptions extends AdminPageOptions {
    status?: Extract<ProductStatus, 'PENDING' | 'APPROVED' | 'REJECTED'>
    keyword?: string
}

export function getAdminProducts(options: AdminProductPageOptions = {}) {
    const { page = 1, signal, status = 'PENDING', keyword = '' } = options
    const query = new URLSearchParams({
        status,
        keyword,
        page: String(page),
        size: String(ADMIN_PAGE_SIZE),
    })
    return apiRequest<AdminProductPage>(
        `/admin/products?${query.toString()}`,
        { signal },
    )
}

export function getPendingProducts(options: AdminPageOptions = {}) {
    return getAdminProducts({ ...options, status: 'PENDING' })
}

export function getAdminProduct(productId: number, signal?: AbortSignal) {
    return apiRequest<AdminProduct>(`/admin/products/${productId}`, { signal })
}

export function updateAdminProductStatus(
    productId: number,
    status: 'APPROVED' | 'REJECTED',
    rejectionReason?: string,
) {
    return apiRequest<AdminProduct>(`/admin/products/${productId}/status`, {
        method: 'PATCH',
        body: { status, rejectionReason },
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
