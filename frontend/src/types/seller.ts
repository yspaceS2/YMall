import type { PageResponse, ProductDetail, ProductSummary } from './product'

export interface SellerProfile {
    sellerProfileId: number
    memberId: number
    storeName: string
    businessNumber: string
    description: string | null
    createdAt: string
    updatedAt: string
}

export interface SellerProfileCreateRequest {
    storeName: string
    businessNumber: string
    description: string
}

export interface SellerProfileUpdateRequest {
    storeName: string
    description: string
}

export type SettlementAccountVerificationStatus = 'UNVERIFIED' | 'VERIFIED'

export interface SellerSettlementAccount {
    settlementAccountId: number
    bankCode: string
    bankName: string
    accountHolder: string
    maskedAccountNumber: string
    verificationStatus: SettlementAccountVerificationStatus
    verifiedAt: string | null
    updatedAt: string
}

export interface SellerSettlementAccountUpsertRequest {
    bankCode: string
    accountHolder: string
    accountNumber: string
    currentPassword: string
}

export type SettlementRequestStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'PAID'

export interface SettlementAvailability {
    entryCount: number
    grossAmount: number
    feeAmount: number
    settlementAmount: number
    hasSettlementAccount: boolean
    canRequest: boolean
}

export interface SettlementRequest {
    settlementRequestId: number
    sellerProfileId: number
    storeName: string
    periodStart: string | null
    periodEnd: string | null
    status: SettlementRequestStatus
    grossAmount: number
    feeAmount: number
    settlementAmount: number
    rejectionReason: string | null
    mockPaymentReference: string | null
    reviewedAt: string | null
    paidAt: string | null
    createdAt: string
    updatedAt: string
}

export interface SettlementRequestHistory {
    fromStatus: SettlementRequestStatus | null
    toStatus: SettlementRequestStatus
    actorMemberId: number
    actorName: string
    reason: string | null
    createdAt: string
}

export interface SellerProductRequest {
    categoryId: number
    name: string
    description: string
    brand: string
    price: number
    discountPercentage: number
    discountStartDate: string | null
    discountEndDate: string | null
    stock: number
    thumbnailUrl: string
    freeShipping: boolean
    shippingFee: number
    estimatedDeliveryDays: number
    images: SellerProductImageRequest[]
    detailImages: SellerProductImageRequest[]
}

export interface SellerProductImageRequest {
    originalUrl: string | null
    imageUrl: string
    sortOrder: number
}

export interface SellerProductSummary extends ProductSummary {
    rejectionReason: string | null
}

export type FulfillmentStatus = 'PENDING' | 'PREPARING' | 'SHIPPED' | 'DELIVERED'

export interface SellerOrderItem {
    orderItemId: number
    productId: number
    productName: string
    unitPrice: number
    quantity: number
    refundedQuantity: number
    lineTotal: number
    thumbnailUrl: string | null
    fulfillmentStatus: FulfillmentStatus
    carrier: string | null
    trackingNumber: string | null
    shippedAt: string | null
    deliveredAt: string | null
}

export interface SellerOrder {
    orderId: number
    orderStatus: string
    sellerAmount: number
    createdAt: string
    refundSupported: boolean
    items: SellerOrderItem[]
}

export interface SellerDeliveryAddress {
    recipientName: string
    recipientPhone: string
    postalCode: string
    roadAddress: string
    detailAddress: string | null
}

export interface SellerOrderDetail extends SellerOrder {
    deliveryAddress: SellerDeliveryAddress | null
}

export interface SellerOrderItemFulfillmentUpdateRequest {
    fulfillmentStatus: FulfillmentStatus
    carrier?: string
    trackingNumber?: string
}

export type SellerProductPage = PageResponse<SellerProductSummary>
export type SellerOrderPage = PageResponse<SellerOrder>
export type SettlementRequestPage = PageResponse<SettlementRequest>
export type SellerProductDetail = ProductDetail
