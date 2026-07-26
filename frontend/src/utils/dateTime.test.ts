import { describe, expect, it } from 'vitest'
import { formatKoreanDate, formatKoreanDateTime } from './dateTime'

describe('한국 시간 표시', () => {
    it('UTC 시각을 Asia/Seoul 기준으로 변환한다', () => {
        const result = formatKoreanDateTime('2026-07-26T12:00:00Z')

        expect(result).toContain('2026')
        expect(result).toContain('오후 9:00')
    })

    it('UTC 자정 경계를 한국 날짜로 변환한다', () => {
        const result = formatKoreanDate('2026-07-31T23:30:00Z')

        expect(result).toContain('2026')
        expect(result).toContain('8. 1.')
    })

    it('올바르지 않은 날짜는 빈 문자열로 반환한다', () => {
        expect(formatKoreanDateTime('invalid')).toBe('')
    })
})
