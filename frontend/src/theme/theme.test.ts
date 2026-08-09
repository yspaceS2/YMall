import { describe, expect, it } from 'vitest'
import { readThemePreference, writeThemePreference } from './theme'

describe('theme storage', () => {
    it('저장소 읽기가 차단되면 시스템 설정을 사용한다', () => {
        const blockedStorage = {
            getItem: () => {
                throw new DOMException('Storage access denied', 'SecurityError')
            },
        }

        expect(readThemePreference(blockedStorage)).toBe('system')
    })

    it('저장소 쓰기가 차단되어도 예외를 전파하지 않는다', () => {
        const blockedStorage = {
            setItem: () => {
                throw new DOMException('Storage access denied', 'SecurityError')
            },
        }

        expect(writeThemePreference('dark', blockedStorage)).toBe(false)
    })
})
