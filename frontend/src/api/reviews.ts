import type { PageResponse } from '../types/api'
import type { Review, ReviewCreateRequest, ReviewUpdateRequest } from '../types/review'
import { apiRequest } from './client'

export function getProductReviews(productId: number, page = 1, size = 10, signal?: AbortSignal) {
    return apiRequest<PageResponse<Review>>(
        `/products/${productId}/reviews?page=${page}&size=${size}`,
        { signal },
    )
}

export function getMyReviews(page = 1, size = 100, signal?: AbortSignal) {
    return apiRequest<PageResponse<Review>>(`/reviews/me?page=${page}&size=${size}`, { signal })
}

export async function getAllMyReviews(signal?: AbortSignal) {
    const reviews: Review[] = []
    let page = 1
    let hasNext = true
    while (hasNext) {
        const response = await getMyReviews(page, 100, signal)
        reviews.push(...response.content)
        hasNext = response.hasNext
        page += 1
    }
    return reviews
}

export function createReview(request: ReviewCreateRequest) {
    return apiRequest<Review>('/reviews', {
        method: 'POST',
        body: request,
    })
}

export function updateReview(reviewId: number, request: ReviewUpdateRequest) {
    return apiRequest<Review>(`/reviews/${reviewId}`, {
        method: 'PUT',
        body: request,
    })
}

export function deleteReview(reviewId: number) {
    return apiRequest<void>(`/reviews/${reviewId}`, { method: 'DELETE' })
}
