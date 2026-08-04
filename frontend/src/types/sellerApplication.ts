import type { PageResponse } from './api'

export type SellerApplicationStatus = 'PENDING' | 'NEEDS_REVISION' | 'APPROVED' | 'REJECTED'

export interface SellerApplication {
    sellerApplicationId: number
    memberId: number
    memberName: string
    memberEmail: string
    storeName: string
    businessNumber: string
    description: string | null
    status: SellerApplicationStatus
    rejectionReason: string | null
    reviewedAt: string | null
    createdAt: string
    updatedAt: string
}

export interface SellerApplicationRequest {
    storeName: string
    businessNumber: string
    description: string
}

export type SellerApplicationPage = PageResponse<SellerApplication>
