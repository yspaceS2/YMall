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
}

export interface SellerProductImageRequest {
    originalUrl: string | null
    imageUrl: string
    sortOrder: number
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

export type SellerProductPage = PageResponse<ProductSummary>
export type SellerOrderPage = PageResponse<SellerOrder>
export type SellerProductDetail = ProductDetail
