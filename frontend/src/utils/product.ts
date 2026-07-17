export function formatPrice(price: number) {
  return `${Math.round(price).toLocaleString('ko-KR')}원`
}

export function getDiscountedPrice(price: number, discountPercentage: number) {
  return price * (1 - discountPercentage / 100)
}

export function resolveImageUrl(url: string) {
  return url
}
