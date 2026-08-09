import type {
  Category,
  PageResponse,
  ProductDetail,
  ProductSuggestion,
  ProductSummary,
} from '../types/product'
import { apiRequest } from './client'

export function getCategories(signal?: AbortSignal) {
  return apiRequest<Category[]>('/categories', { signal })
}

export function getProducts(options: {
  page: number
  size: number
  keyword?: string
  categoryId?: number
  signal?: AbortSignal
}) {
  const params = new URLSearchParams({
    page: String(options.page),
    size: String(options.size),
  })

  let path = '/products'
  if (options.keyword) {
    path = '/products/search'
    params.set('keyword', options.keyword)
    if (options.categoryId) params.set('categoryId', String(options.categoryId))
  } else if (options.categoryId) {
    path = `/categories/${options.categoryId}/products`
  }

  return apiRequest<PageResponse<ProductSummary>>(`${path}?${params}`, {
    signal: options.signal,
  })
}

export function getProduct(productId: number, signal?: AbortSignal) {
  return apiRequest<ProductDetail>(`/products/${productId}`, { signal })
}

export function getProductSuggestions(
  keyword: string,
  size = 8,
  categoryId?: number,
  signal?: AbortSignal,
) {
  const params = new URLSearchParams({
    keyword,
    size: String(size),
  })
  if (categoryId) params.set('categoryId', String(categoryId))
  return apiRequest<ProductSuggestion[]>(`/products/suggestions?${params}`, { signal })
}
