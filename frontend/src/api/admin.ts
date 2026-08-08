import type {
    AdminCategory,
    AdminCategoryRequest,
    AdminAuthorization,
    AdminMember,
    AdminMemberPage,
    AdminAuditLog,
    AdminGrade,
    MemberAccessStatus,
    AdminRoleUpdateRequest,
    AdminRoleUpdateResponse,
    AdminOrder,
    AdminOrderPage,
    AdminProduct,
    AdminProductPage,
    ProductChangeRequest,
    ProductChangeRequestStatus,
    AdminSellerPage,
    AdminSeller,
    AdminSettlementRequest,
    AdminSettlementRequestPage,
    SettlementRequestStatus,
} from '../types/admin'
import { apiRequest } from './client'
import type { PaymentRefund, PaymentRefundRequest } from '../types/order'
import type { ProductStatus } from '../types/product'
import type { SettlementRequestHistory, SettlementRequestWorkType } from '../types/seller'
import {
    buildSettlementRequestQuery,
    type SettlementRequestQueryOptions,
} from './settlementQuery'

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
    keyword?: string
}

export interface AdminMemberPageOptions extends AdminPageOptions {
    status?: MemberAccessStatus
    role?: 'ROLE_USER' | 'ROLE_SELLER' | 'ROLE_ADMIN'
    adminGrade?: AdminGrade
    joinedFrom?: string
    joinedTo?: string
}

export type AdminOrderWorkType = 'PENDING_REFUND' | 'PENDING_RETURN'

interface AdminOrderPageOptions extends AdminPageOptions {
    workType?: AdminOrderWorkType
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

export function getAdminProductChangeRequests(
    status: ProductChangeRequestStatus = 'PENDING',
    page = 1,
    signal?: AbortSignal,
) {
    const query = new URLSearchParams({
        status,
        page: String(page),
        size: String(ADMIN_PAGE_SIZE),
    })
    return apiRequest<import('../types/api').PageResponse<ProductChangeRequest>>(
        `/admin/product-change-requests?${query.toString()}`,
        { signal },
    )
}

export function getAdminProductChangeRequest(
    requestId: number,
    signal?: AbortSignal,
) {
    return apiRequest<ProductChangeRequest>(
        `/admin/product-change-requests/${requestId}`,
        { signal },
    )
}

export function reviewAdminProductChangeRequest(
    requestId: number,
    status: Extract<ProductChangeRequestStatus, 'APPROVED' | 'REJECTED'>,
    rejectionReason?: string,
) {
    return apiRequest<ProductChangeRequest>(
        `/admin/product-change-requests/${requestId}/status`,
        {
            method: 'PATCH',
            body: { status, rejectionReason },
        },
    )
}

export function getAdminMembers(options: AdminMemberPageOptions = {}) {
    const {
        page = 1,
        signal,
        keyword = '',
        status,
        role,
        adminGrade,
        joinedFrom,
        joinedTo,
    } = options
    const query = new URLSearchParams({
        page: String(page),
        size: String(ADMIN_PAGE_SIZE),
        keyword,
    })
    if (status) query.set('status', status)
    if (role) query.set('role', role)
    if (adminGrade) query.set('adminGrade', adminGrade)
    if (joinedFrom) query.set('joinedFrom', joinedFrom)
    if (joinedTo) query.set('joinedTo', joinedTo)
    return apiRequest<AdminMemberPage>(`/admin/members?${query.toString()}`, { signal })
}

export function updateAdminMemberRestriction(
    memberId: number,
    restricted: boolean,
    reason: string,
) {
    return apiRequest<AdminMember>(`/admin/members/${memberId}/restriction`, {
        method: 'PATCH',
        body: { restricted, reason },
    })
}

export function revokeAdminMemberSessions(memberId: number, reason: string) {
    return apiRequest<void>(`/admin/members/${memberId}/sessions/revoke`, {
        method: 'POST',
        body: { reason },
    })
}

export function getAdminMemberAuditLogs(memberId: number, signal?: AbortSignal) {
    return apiRequest<AdminAuditLog[]>(`/admin/members/${memberId}/audit-logs`, { signal })
}

export function getAdminMember(memberId: number, signal?: AbortSignal) {
    return apiRequest<AdminMember>(`/admin/members/${memberId}`, { signal })
}

export function getAdminAuthorization(signal?: AbortSignal) {
    return apiRequest<AdminAuthorization>('/admin/authorization', { signal })
}

export function updateAdminMemberRole(
    memberId: number,
    request: AdminRoleUpdateRequest,
) {
    return apiRequest<AdminRoleUpdateResponse>(
        `/admin/members/${memberId}/admin-role`,
        { method: 'PATCH', body: request },
    )
}

export function getAdminSellers(options: AdminPageOptions = {}) {
    const { page = 1, signal, keyword = '' } = options
    const query = new URLSearchParams({
        page: String(page),
        size: String(ADMIN_PAGE_SIZE),
        keyword,
    })
    return apiRequest<AdminSellerPage>(`/admin/sellers?${query.toString()}`, { signal })
}

export function getAdminSeller(sellerId: number, signal?: AbortSignal) {
    return apiRequest<AdminSeller>(`/admin/sellers/${sellerId}`, { signal })
}

export function getAdminOrders(options: AdminOrderPageOptions = {}) {
    const { page = 1, signal, keyword = '', workType } = options
    const query = new URLSearchParams({
        page: String(page),
        size: String(ADMIN_PAGE_SIZE),
        keyword,
    })
    if (workType) query.set('workType', workType)
    return apiRequest<AdminOrderPage>(`/admin/orders?${query.toString()}`, { signal })
}

export function getAdminOrder(orderId: number, signal?: AbortSignal) {
    return apiRequest<AdminOrder>(`/admin/orders/${orderId}`, { signal })
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

type AdminSettlementRequestOptions = Omit<
    SettlementRequestQueryOptions,
    'status' | 'workType'
> & {
    status?: SettlementRequestStatus
    workType?: SettlementRequestWorkType
    signal?: AbortSignal
}

export function getAdminSettlementRequests(
    options: AdminSettlementRequestOptions = {},
) {
    const { signal, ...queryOptions } = options
    const query = buildSettlementRequestQuery(queryOptions)
    return apiRequest<AdminSettlementRequestPage>(
        `/admin/settlement-requests?${query}`,
        { signal },
    )
}

export function getAdminSettlementRequest(
    settlementRequestId: number,
    signal?: AbortSignal,
) {
    return apiRequest<AdminSettlementRequest>(
        `/admin/settlement-requests/${settlementRequestId}`,
        { signal },
    )
}

export function getAdminSettlementRequestHistories(
    settlementRequestId: number,
    signal?: AbortSignal,
) {
    return apiRequest<SettlementRequestHistory[]>(
        `/admin/settlement-requests/${settlementRequestId}/histories`,
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
