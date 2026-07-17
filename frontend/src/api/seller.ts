import type {
    SellerOrder,
    SellerOrderPage,
    SellerProductDetail,
    SellerProductPage,
    SellerProductRequest,
    SellerProfile,
    SellerProfileCreateRequest,
    SellerProfileUpdateRequest,
    FulfillmentStatus,
} from '../types/seller'
import { apiRequest } from './client'

export function getSellerProfile(signal?: AbortSignal) {
    return apiRequest<SellerProfile>('/seller/profile', { signal })
}

export function createSellerProfile(request: SellerProfileCreateRequest) {
    return apiRequest<SellerProfile>('/seller/profile', { method: 'POST', body: request })
}

export function updateSellerProfile(request: SellerProfileUpdateRequest) {
    return apiRequest<SellerProfile>('/seller/profile', { method: 'PUT', body: request })
}

export function getSellerProducts(signal?: AbortSignal) {
    return apiRequest<SellerProductPage>('/seller/products?page=1&size=100', { signal })
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

export function getSellerOrders(signal?: AbortSignal) {
    return apiRequest<SellerOrderPage>('/seller/orders?page=0&size=100', { signal })
}

export function updateSellerOrderStatus(orderId: number, fulfillmentStatus: FulfillmentStatus) {
    return apiRequest<SellerOrder>(`/seller/orders/${orderId}/status`, {
        method: 'PATCH',
        body: { fulfillmentStatus },
    })
}
