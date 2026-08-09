import type { PageResponse } from '../types/api'
import type {
    Order,
    OrderCreateRequest,
    PaymentConfirmRequest,
    PaymentRefund,
    PaymentRefundRequest,
    PaymentResponse,
    ReturnRequest,
    ReturnRequestCreateRequest,
} from '../types/order'
import { apiRequest } from './client'

export function createOrder(request: OrderCreateRequest) {
    return apiRequest<Order>('/orders', {
        method: 'POST',
        body: request,
    })
}

export function getOrder(orderId: number, signal?: AbortSignal) {
    return apiRequest<Order>(`/orders/${orderId}`, { signal })
}

export function getOrders(page = 1, size = 20, signal?: AbortSignal) {
    return apiRequest<PageResponse<Order>>(`/orders?page=${page}&size=${size}`, { signal })
}

export function confirmPayment(orderId: number, request: PaymentConfirmRequest) {
    return apiRequest<PaymentResponse>(`/orders/${orderId}/payments/confirmations`, {
        method: 'POST',
        body: request,
    })
}

export function cancelOrder(orderId: number) {
    return apiRequest<Order>(`/orders/${orderId}/cancellations`, {
        method: 'POST',
    })
}

export function requestRefund(orderId: number, request: PaymentRefundRequest) {
    return apiRequest<PaymentRefund>(`/orders/${orderId}/refunds`, {
        method: 'POST',
        body: request,
    })
}

export function getRefunds(orderId: number, signal?: AbortSignal) {
    return apiRequest<PaymentRefund[]>(`/orders/${orderId}/refunds`, { signal })
}

export function createReturnRequest(
    orderId: number,
    request: ReturnRequestCreateRequest,
) {
    return apiRequest<ReturnRequest>(`/orders/${orderId}/return-requests`, {
        method: 'POST',
        body: request,
    })
}

export function getReturnRequests(orderId: number, signal?: AbortSignal) {
    return apiRequest<ReturnRequest[]>(`/orders/${orderId}/return-requests`, { signal })
}
