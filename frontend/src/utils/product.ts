import type { ProductStatus } from '../types/product'

const productStatusLabels: Record<ProductStatus, string> = {
  DRAFT: '임시 저장',
  PENDING: '승인 대기',
  APPROVED: '판매 중',
  REJECTED: '승인 반려',
  SOLD_OUT: '품절',
  DELETED: '삭제',
}

export function formatPrice(price: number) {
  return `${Math.round(price).toLocaleString('ko-KR')}원`
}

export function getDiscountedPrice(price: number, discountPercentage: number) {
  return price * (1 - discountPercentage / 100)
}

export function resolveImageUrl(url: string) {
  return url
}

export function getProductStatusLabel(status: ProductStatus) {
  return productStatusLabels[status]
}
