import type { MemberRole } from './auth'
import type { OrderStatus } from './order'
import type { ProductDetailImage, ProductImage, ProductStatus } from './product'
import type {
    FulfillmentStatus,
    SettlementRequest,
    SettlementRequestPage,
    SettlementRequestStatus,
} from './seller'
import type { PageResponse } from './api'

export interface AdminProduct {
    productId: number
    sellerProfileId: number | null
    storeName: string | null
    categoryName: string
    name: string
    description: string | null
    brand: string | null
    price: number
    discountPercentage: number
    discountStartDate: string | null
    discountEndDate: string | null
    stock: number
    thumbnailUrl: string | null
    freeShipping: boolean
    shippingFee: number
    estimatedDeliveryDays: number
    images: ProductImage[]
    detailImages: ProductDetailImage[]
    status: ProductStatus
    rejectionReason: string | null
    createdAt: string
}

export type ProductChangeRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface ProductSnapshot {
    categoryId: number
    categoryName: string
    name: string
    description: string | null
    brand: string | null
    price: number
    discountPercentage: number
    discountStartDate: string | null
    discountEndDate: string | null
    stock: number
    thumbnailUrl: string | null
    freeShipping: boolean
    shippingFee: number
    estimatedDeliveryDays: number
    images: Array<{ originalUrl: string | null; imageUrl: string; sortOrder: number }>
    detailImages: Array<{ originalUrl: string | null; imageUrl: string; sortOrder: number }>
}

export interface ProductChangeRequest {
    productChangeRequestId: number
    productId: number
    sellerProfileId: number | null
    storeName: string | null
    status: ProductChangeRequestStatus
    current: ProductSnapshot
    proposed: ProductSnapshot
    rejectionReason: string | null
    createdAt: string
    reviewedAt: string | null
}

export interface AdminCategory {
  categoryId: number
  name: string
  slug: string
  parentId: number | null
  parentName: string | null
  depth: number
  displayOrder: number
  active: boolean
  hasChildren: boolean
  hasProducts: boolean
  createdAt: string
  updatedAt: string
}

export interface AdminCategoryRequest {
  name: string
  slug: string
  parentId: number | null
  displayOrder: number
  active: boolean
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
    refundedQuantity: number
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
    refundSupported: boolean
    createdAt: string
}

export type AdminProductPage = PageResponse<AdminProduct>
export type AdminMemberPage = PageResponse<AdminMember>
export type AdminSellerPage = PageResponse<AdminSeller>
export type AdminOrderPage = PageResponse<AdminOrder>
export type {
    SettlementRequest as AdminSettlementRequest,
    SettlementRequestPage as AdminSettlementRequestPage,
    SettlementRequestStatus,
}
