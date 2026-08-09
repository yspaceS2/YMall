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
    adminGrade: AdminGrade | null
    accessStatus: MemberAccessStatus
    lastLoginAt: string | null
    restrictionReason: string | null
    restrictedAt: string | null
    restrictedByMemberId: number | null
    orderCount: number
    totalPaidAmount: number
    createdAt: string
}

export type MemberAccessStatus = 'ACTIVE' | 'RESTRICTED'

export type AdminGrade = 'MANAGER' | 'SUPERVISOR' | 'SUPER_ADMIN'

export type AdminPermission =
    | 'DASHBOARD_READ'
    | 'MEMBER_READ'
    | 'MEMBER_RESTRICT_LIMITED'
    | 'MEMBER_RESTRICT_ALL'
    | 'SELLER_READ'
    | 'SELLER_APPLICATION_REVIEW'
    | 'SELLER_APPLICATION_DECIDE'
    | 'SUPPORT_REPLY'
    | 'PRODUCT_REVIEW'
    | 'REFUND_STANDARD'
    | 'REFUND_ALL'
    | 'SETTLEMENT_REVIEW'
    | 'SETTLEMENT_APPROVE'
    | 'TASK_SELF'
    | 'TASK_ASSIGN'
    | 'CATEGORY_READ'
    | 'CATEGORY_MANAGE_PARTIAL'
    | 'CATEGORY_MANAGE_ALL'
    | 'ADMIN_MANAGER_MANAGE'
    | 'ADMIN_ALL_MANAGE'
    | 'AUDIT_OWN_READ'
    | 'AUDIT_ALL_READ'

export interface AdminAuthorization {
    memberId: number
    adminGrade: AdminGrade
    permissions: AdminPermission[]
}

export interface AdminRoleUpdateRequest {
    role: Extract<MemberRole, 'ROLE_USER' | 'ROLE_ADMIN'>
    adminGrade: AdminGrade | null
    reason: string
}

export interface AdminRoleUpdateResponse {
    memberId: number
    role: MemberRole
    adminGrade: AdminGrade | null
    permissions: AdminPermission[]
}

export interface AdminAuditLog {
    auditLogId: number
    actorMemberId: number
    actorName: string
    actorGrade: AdminGrade
    action: 'ADMIN_ROLE_CHANGED' | 'MEMBER_RESTRICTION_CHANGED' | 'MEMBER_SESSIONS_REVOKED'
    beforeValue: string | null
    afterValue: string | null
    reason: string
    createdAt: string
}

export interface AdminSeller {
    sellerProfileId: number
    memberId: number
    email: string
    memberName: string
    storeName: string
    businessNumber: string
    productCount: number
    pendingProductCount: number
    orderCount: number
    grossSalesAmount: number
    refundedQuantity: number
    pendingReturnCount: number
    pendingSupportCount: number
    pendingSettlementCount: number
    applicationStatus: 'PENDING' | 'NEEDS_REVISION' | 'APPROVED' | 'REJECTED' | null
    applicationReviewReason: string | null
    applicationReviewedAt: string | null
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
