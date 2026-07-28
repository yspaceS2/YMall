export type ProductStatus =
  | 'DRAFT'
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'SOLD_OUT'
  | 'DELETED'

export interface Category {
  categoryId: number
  name: string
  slug: string
}

export interface ProductSummary {
  productId: number
  categoryId: number
  categoryName: string
  name: string
  brand: string
  price: number
  discountPercentage: number
  rating: number | null
  stock: number
  thumbnailUrl: string | null
  status: ProductStatus
}

export interface ProductImage {
  imageId: number
  originalUrl: string
  imageUrl: string
  sortOrder: number
}

export interface ProductDetailImage {
  detailImageId: number
  originalUrl: string | null
  imageUrl: string
  sortOrder: number
}

export interface ProductDetail extends Omit<ProductSummary, 'categoryId' | 'categoryName'> {
  category: Category
  description: string
  images: ProductImage[]
  detailImages: ProductDetailImage[]
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}
