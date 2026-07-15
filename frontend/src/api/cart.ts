import type {
    CartItem,
    CartItemAddRequest,
    CartItemQuantityUpdateRequest,
    CartResponse,
} from '../types/cart'
import { apiRequest } from './client'

export function getCart(signal?: AbortSignal) {
    return apiRequest<CartResponse>('/cart', { signal })
}

export function addCartItem(request: CartItemAddRequest) {
    return apiRequest<CartItem>('/cart/items', {
        method: 'POST',
        body: request,
    })
}

export function updateCartItemQuantity(
    cartItemId: number,
    request: CartItemQuantityUpdateRequest,
) {
    return apiRequest<CartItem>(`/cart/items/${cartItemId}`, {
        method: 'PATCH',
        body: request,
    })
}

export function deleteCartItem(cartItemId: number) {
    return apiRequest<void>(`/cart/items/${cartItemId}`, {
        method: 'DELETE',
    })
}
