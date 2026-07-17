import type {
    AdminMemberPage,
    AdminOrderPage,
    AdminProduct,
    AdminProductPage,
    AdminSellerPage,
} from '../types/admin'
import { apiRequest } from './client'

const ADMIN_PAGE_SIZE = 20

export function getPendingProducts(signal?: AbortSignal) {
    return apiRequest<AdminProductPage>(
        `/admin/products?status=PENDING&page=1&size=${ADMIN_PAGE_SIZE}`,
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

export function getAdminMembers(signal?: AbortSignal) {
    return apiRequest<AdminMemberPage>(`/admin/members?page=1&size=${ADMIN_PAGE_SIZE}`, { signal })
}

export function getAdminSellers(signal?: AbortSignal) {
    return apiRequest<AdminSellerPage>(`/admin/sellers?page=1&size=${ADMIN_PAGE_SIZE}`, { signal })
}

export function getAdminOrders(signal?: AbortSignal) {
    return apiRequest<AdminOrderPage>(`/admin/orders?page=1&size=${ADMIN_PAGE_SIZE}`, { signal })
}
