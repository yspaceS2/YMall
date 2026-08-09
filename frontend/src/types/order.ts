export type OrderStatus =
    | 'PENDING_PAYMENT'
    | 'PAID'
    | 'PAYMENT_FAILED'
    | 'CANCELED'
    | 'PARTIALLY_REFUNDED'
    | 'REFUNDED'
    | 'PREPARING'
    | 'SHIPPED'
    | 'DELIVERED'

export type OrderItemFulfillmentStatus = 'PENDING' | 'PREPARING' | 'SHIPPED' | 'DELIVERED'

export interface OrderItem {
    orderItemId: number
    productId: number
    productName: string
    thumbnailUrl: string | null
    unitPrice: number
    quantity: number
    refundedQuantity: number
    totalPrice: number
    shippingFee: number
    fulfillmentStatus: OrderItemFulfillmentStatus
}

export interface Order {
    orderId: number
    paymentOrderId: string
    status: OrderStatus
    totalAmount: number
    productAmount: number
    shippingFee: number
    items: OrderItem[]
    deliveryAddress: OrderDeliveryAddress | null
    refundSupported: boolean
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

export type PaymentRefundType = 'FULL' | 'PARTIAL'
export type PaymentRefundStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'UNKNOWN'

export interface PaymentRefundItemRequest {
    orderItemId: number
    quantity: number
}

export interface PaymentRefundRequest {
    idempotencyKey: string
    reason: string
    items?: PaymentRefundItemRequest[]
}

export interface PaymentRefundItem {
    orderItemId: number
    productName: string
    quantity: number
    amount: number
}

export interface PaymentRefund {
    refundId: number
    orderId: number
    type: PaymentRefundType
    status: PaymentRefundStatus
    amount: number
    reason: string
    failureMessage: string | null
    items: PaymentRefundItem[]
    createdAt: string
    processedAt: string | null
}

export type ReturnRequestStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED'

export interface ReturnRequestCreateRequest {
    orderItemId: number
    quantity: number
    reason: string
}

export interface ReturnRequest {
    returnRequestId: number
    orderId: number
    orderItemId: number
    productId: number
    productName: string
    thumbnailUrl: string | null
    memberName: string
    quantity: number
    reason: string
    status: ReturnRequestStatus
    sellerResponse: string | null
    paymentRefundId: number | null
    returnDeadline: string | null
    requestedAt: string
    processedAt: string | null
}
