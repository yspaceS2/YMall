import type { PageResponse } from '../types/api'
import type {
    MockPaymentRequest,
    Order,
    OrderCreateRequest,
    PaymentResponse,
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

export function processMockPayment(orderId: number, request: MockPaymentRequest) {
    return apiRequest<PaymentResponse>(`/orders/${orderId}/payments`, {
        method: 'POST',
        body: request,
    })
}

export function cancelOrder(orderId: number) {
    return apiRequest<Order>(`/orders/${orderId}/cancellations`, {
        method: 'POST',
    })
}
