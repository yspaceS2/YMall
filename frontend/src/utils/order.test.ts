import { describe, expect, it } from 'vitest'
import { getOrderItemFulfillmentStatusLabel, getOrderStatusLabel } from './order'

describe('order presentation', () => {
    it('주문 상태를 한글로 표시한다', () => {
        expect(getOrderStatusLabel('PAID')).toBe('결제 완료')
        expect(getOrderStatusLabel('PARTIALLY_REFUNDED')).toBe('부분 환불')
        expect(getOrderStatusLabel('REFUNDED')).toBe('환불 완료')
    })

    it('상품 처리 상태를 한글로 표시한다', () => {
        expect(getOrderItemFulfillmentStatusLabel('PENDING')).toBe('처리 대기')
        expect(getOrderItemFulfillmentStatusLabel('PREPARING')).toBe('상품 준비 중')
        expect(getOrderItemFulfillmentStatusLabel('SHIPPED')).toBe('배송 중')
        expect(getOrderItemFulfillmentStatusLabel('DELIVERED')).toBe('배송 완료')
    })
})
