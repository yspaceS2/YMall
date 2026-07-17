import type { MemberRole } from './auth'
import type { OrderStatus } from './order'
import type { ProductStatus } from './product'
import type { FulfillmentStatus } from './seller'
import type { PageResponse } from './api'

export interface AdminProduct {
    productId: number
    sellerProfileId: number | null
    storeName: string | null
    categoryName: string
    name: string
    brand: string | null
    price: number
    stock: number
    thumbnailUrl: string | null
    status: ProductStatus
    createdAt: string
}

export interface AdminMember {
    memberId: number
    email: string
    name: string
    role: MemberRole
    createdAt: string
}

export interface AdminSeller {
    sellerProfileId: number
    memberId: number
    email: string
    memberName: string
    storeName: string
    businessNumber: string
    createdAt: string
}

export interface AdminOrderItem {
    orderItemId: number
    productId: number
    productName: string
    unitPrice: number
    quantity: number
    lineTotal: number
    fulfillmentStatus: FulfillmentStatus
}

export interface AdminOrder {
    orderId: number
    memberId: number
    memberEmail: string
    memberName: string
    status: OrderStatus
    totalAmount: number
    items: AdminOrderItem[]
    createdAt: string
}

export type AdminProductPage = PageResponse<AdminProduct>
export type AdminMemberPage = PageResponse<AdminMember>
export type AdminSellerPage = PageResponse<AdminSeller>
export type AdminOrderPage = PageResponse<AdminOrder>
