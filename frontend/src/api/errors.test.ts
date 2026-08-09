import { describe, expect, it } from 'vitest'
import { ApiError } from './client'
import { getApiErrorMessage, isAbortError } from './errors'

describe('API error helpers', () => {
    it('AbortError를 요청 취소로 판별한다', () => {
        const abortError = new Error('aborted')
        abortError.name = 'AbortError'

        expect(isAbortError(abortError)).toBe(true)
        expect(isAbortError(new Error('network error'))).toBe(false)
    })

    it('ApiError 메시지를 우선하고 그 외 오류에는 fallback을 사용한다', () => {
        expect(getApiErrorMessage(
            new ApiError('상품을 찾을 수 없습니다.', 404, 'PRODUCT_NOT_FOUND'),
            '상품 조회에 실패했습니다.',
        )).toBe('상품을 찾을 수 없습니다.')
        expect(getApiErrorMessage(
            new Error('internal message'),
            '상품 조회에 실패했습니다.',
        )).toBe('상품 조회에 실패했습니다.')
    })
})
