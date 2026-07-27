import type { PageResponse, ProductStatus } from './product'

export interface WishlistStatus {
    productId: number
    wished: boolean
}

export interface WishlistProduct {
    productId: number
    name: string
    brand: string
    price: number
    discountPercentage: number
    rating: number | null
    stock: number
    thumbnailUrl: string | null
    status: ProductStatus
    wishedAt: string
}

export type WishlistPage = PageResponse<WishlistProduct>
