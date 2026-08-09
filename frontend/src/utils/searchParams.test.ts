import { describe, expect, it } from 'vitest'
import { parsePositiveInteger } from './searchParams'

describe('parsePositiveInteger', () => {
    it.each([
        ['3', 3],
        ['1', 1],
    ])('양의 정수 문자열 %s를 숫자로 변환한다', (value, expected) => {
        expect(parsePositiveInteger(value, 9)).toBe(expected)
    })

    it.each([null, '', '0', '-1', '1.5', 'invalid'])(
        '유효하지 않은 값 %s에는 fallback을 사용한다',
        (value) => {
            expect(parsePositiveInteger(value, 9)).toBe(9)
        },
    )
})
