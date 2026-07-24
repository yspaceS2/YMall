import type {
    AdminMemberPage,
    AdminOrderPage,
    AdminProduct,
    AdminProductPage,
    AdminSellerPage,
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
