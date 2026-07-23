export type OrderStatus =
    | 'PENDING_PAYMENT'
    | 'PAID'
    | 'PAYMENT_FAILED'
    | 'CANCELED'
    | 'PREPARING'
    | 'SHIPPED'
    | 'DELIVERED'

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
    paymentOrderId: string
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

export interface PaymentConfirmRequest {
    paymentKey: string
    paymentOrderId: string
    amount: number
    idempotencyKey: string
}

export interface PaymentResponse {
    paymentId: number
    orderId: number
    paymentKey: string | null
    paymentOrderId: string
    requestedAmount: number
    approvedAmount: number | null
    method: string | null
    approvedAt: string | null
    result: 'SUCCESS' | 'FAILURE'
    orderStatus: OrderStatus
    failureCode: string | null
    failureMessage: string | null
    processedAt: string
}
