import type { Category, PageResponse, ProductDetail, ProductSummary } from '../types/product'
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
