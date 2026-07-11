import type { Category, PageResponse, ProductDetail, ProductSummary } from '../types/product'

interface ApiResponse<T> {
  success: boolean
  data: T
  message: string
}

interface ErrorResponse {
  error?: {
    message?: string
  }
}

async function request<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(path, { signal })

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as ErrorResponse | null
    throw new Error(error?.error?.message ?? '요청을 처리하지 못했습니다.')
  }

  const body = (await response.json()) as ApiResponse<T>
  return body.data
}

export function getCategories(signal?: AbortSignal) {
  return request<Category[]>('/api/categories', signal)
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

  let path = '/api/products'
  if (options.keyword) {
    path = '/api/products/search'
    params.set('keyword', options.keyword)
  } else if (options.categoryId) {
    path = `/api/categories/${options.categoryId}/products`
  }

  return request<PageResponse<ProductSummary>>(`${path}?${params}`, options.signal)
}

export function getProduct(productId: number, signal?: AbortSignal) {
  return request<ProductDetail>(`/api/products/${productId}`, signal)
}
