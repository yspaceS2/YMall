import type { WishlistPage, WishlistStatus } from '../types/wishlist'
import { apiRequest } from './client'

export function getWishlistStatus(productId: number, signal?: AbortSignal) {
    return apiRequest<WishlistStatus>(
        `/members/me/wishlist/products/${productId}`,
        { signal },
    )
}

export function addWishlistProduct(productId: number) {
    return apiRequest<WishlistStatus>(
        `/members/me/wishlist/products/${productId}`,
        { method: 'POST' },
    )
}

export function removeWishlistProduct(productId: number) {
    return apiRequest<void>(
        `/members/me/wishlist/products/${productId}`,
        { method: 'DELETE' },
    )
}

export function getWishlist(page = 1, size = 8, signal?: AbortSignal) {
    const params = new URLSearchParams({
        page: String(page),
        size: String(size),
    })
    return apiRequest<WishlistPage>(`/members/me/wishlist?${params}`, { signal })
}
