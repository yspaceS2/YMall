import { describe, expect, it } from 'vitest'
import { getProductStatusLabel } from './product'

describe('getProductStatusLabel', () => {
    it.each([
        ['DRAFT', '임시 저장'],
        ['PENDING', '승인 대기'],
        ['APPROVED', '판매 중'],
        ['REJECTED', '승인 반려'],
        ['SOLD_OUT', '품절'],
        ['DELETED', '삭제'],
    ] as const)('%s 상태를 한글로 표시한다', (status, label) => {
        expect(getProductStatusLabel(status)).toBe(label)
    })
})
