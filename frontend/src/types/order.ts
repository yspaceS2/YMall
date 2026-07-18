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
    deliveryAddress: OrderDeliveryAddress | null
    createdAt: string
}

export interface OrderDeliveryAddress {
    recipientName: string
    recipientPhone: string
    postalCode: string
    roadAddress: string
    detailAddress: string
}

export interface OrderCreateRequest {
    idempotencyKey: string
    addressId?: number
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
