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
    periodStart: string
    periodEnd: string
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
    periodStart: string
    periodEnd: string
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

export interface SellerProductRequest {
    categoryId: number
    name: string
    description: string
    brand: string
    price: number
    discountPercentage: number
    stock: number
    thumbnailUrl: string
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
    fulfillmentStatus: FulfillmentStatus
}

export interface SellerOrder {
    orderId: number
    orderStatus: string
    sellerAmount: number
    createdAt: string
    refundSupported: boolean
    items: SellerOrderItem[]
}

export type SellerProductPage = PageResponse<SellerProductSummary>
export type SellerOrderPage = PageResponse<SellerOrder>
export type SettlementRequestPage = PageResponse<SettlementRequest>
export type SellerProductDetail = ProductDetail
