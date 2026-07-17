export interface Review {
    reviewId: number
    orderItemId: number
    productId: number
    authorName: string
    rating: number
    content: string
    createdAt: string
    updatedAt: string
}

export interface ReviewCreateRequest {
    orderItemId: number
    rating: number
    content: string
}

export interface ReviewUpdateRequest {
    rating: number
    content: string
}
