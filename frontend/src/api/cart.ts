import type {
    CartItem,
    CartItemAddRequest,
    CartItemQuantityUpdateRequest,
    CartResponse,
} from '../types/cart'
import { apiRequest } from './client'

export const CART_CHANGED_EVENT = 'ymall:cart-changed'

function notifyCartChanged() {
    window.dispatchEvent(new Event(CART_CHANGED_EVENT))
}

export function getCart(signal?: AbortSignal) {
    return apiRequest<CartResponse>('/cart', { signal })
}

export async function addCartItem(request: CartItemAddRequest) {
    const item = await apiRequest<CartItem>('/cart/items', {
        method: 'POST',
        body: request,
    })
    notifyCartChanged()
    return item
}

export async function updateCartItemQuantity(
    cartItemId: number,
    request: CartItemQuantityUpdateRequest,
) {
    const item = await apiRequest<CartItem>(`/cart/items/${cartItemId}`, {
        method: 'PATCH',
        body: request,
    })
    notifyCartChanged()
    return item
}

export async function deleteCartItem(cartItemId: number) {
    await apiRequest<void>(`/cart/items/${cartItemId}`, {
        method: 'DELETE',
    })
    notifyCartChanged()
}
