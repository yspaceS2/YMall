import type {
    SellerApplication,
    SellerApplicationPage,
    SellerApplicationRequest,
    SellerApplicationStatus,
} from '../types/sellerApplication'
import { apiRequest } from './client'

export function getMySellerApplication(signal?: AbortSignal) {
    return apiRequest<SellerApplication>('/members/seller-application', { signal })
}

export function createSellerApplication(request: SellerApplicationRequest) {
    return apiRequest<SellerApplication>('/members/seller-application', {
        method: 'POST',
        body: request,
    })
}

export function getAdminSellerApplications(
    status: SellerApplicationStatus = 'PENDING',
    options: {
        page?: number
        size?: number
        keyword?: string
        signal?: AbortSignal
    } = {},
) {
    const { page = 1, size = 20, keyword = '', signal } = options
    const query = new URLSearchParams({
        status,
        page: String(page),
        size: String(size),
        keyword,
    })
    return apiRequest<SellerApplicationPage>(
        `/admin/seller-applications?${query.toString()}`,
        { signal },
    )
}

export function getAdminSellerApplication(
    sellerApplicationId: number,
    signal?: AbortSignal,
) {
    return apiRequest<SellerApplication>(
        `/admin/seller-applications/${sellerApplicationId}`,
        { signal },
    )
}

export function reviewSellerApplication(
    sellerApplicationId: number,
    status: Extract<SellerApplicationStatus, 'NEEDS_REVISION' | 'APPROVED' | 'REJECTED'>,
    rejectionReason?: string,
) {
    return apiRequest<SellerApplication>(
        `/admin/seller-applications/${sellerApplicationId}`,
        {
            method: 'PATCH',
            body: { status, rejectionReason },
        },
    )
}
