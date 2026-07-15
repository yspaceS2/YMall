import type { ProductStatus } from './product'

export interface CartItem {
    cartItemId: number
    productId: number
    productName: string
    thumbnailUrl: string | null
    price: number
    discountPercentage: number
    stock: number
    productStatus: ProductStatus
    quantity: number
}

export interface CartResponse {
    items: CartItem[]
}

export interface CartItemAddRequest {
    productId: number
    quantity: number
}

export interface CartItemQuantityUpdateRequest {
    quantity: number
}
