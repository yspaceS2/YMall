export interface HomeMerchandisingProduct {
    productId: number
    categoryId: number
    categoryName: string
    name: string
    brand: string
    price: number
    discountPercentage: number
    rating: number | null
    reviewCount: number
    thumbnailUrl: string | null
    salesQuantity: number
}

export interface HomeMerchandisingGroup {
    categoryId: number
    categoryName: string
    categorySlug: string
    products: HomeMerchandisingProduct[]
}

export interface HomeMerchandising {
    categoryBest: HomeMerchandisingGroup[]
    grocery: HomeMerchandisingGroup[]
    fashion: HomeMerchandisingGroup[]
    newArrivals: HomeMerchandisingProduct[]
}
