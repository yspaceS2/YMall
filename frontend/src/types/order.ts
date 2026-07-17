export type OrderStatus =
    | 'PENDING_PAYMENT'
    | 'PAID'
    | 'PAYMENT_FAILED'
    | 'CANCELED'
    | 'PREPARING'
    | 'SHIPPED'
    | 'DELIVERED'

export type PaymentResult = 'SUCCESS' | 'FAILURE'

export type OrderItemFulfillmentStatus = 'PENDING' | 'PREPARING' | 'SHIPPED' | 'DELIVERED'

export interface OrderItem {
    orderItemId: number
    productId: number
    productName: string
    unitPrice: number
    quantity: number
    totalPrice: number
    fulfillmentStatus: OrderItemFulfillmentStatus
}

export interface Order {
    orderId: number
    status: OrderStatus
    totalAmount: number
    items: OrderItem[]
    createdAt: string
}

export interface OrderCreateRequest {
    idempotencyKey: string
}

export interface MockPaymentRequest {
    idempotencyKey: string
    result: PaymentResult
}

export interface PaymentResponse {
    paymentId: number
    orderId: number
    result: PaymentResult
    orderStatus: OrderStatus
    failureMessage: string | null
    processedAt: string
}
