import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ThemeSelector } from '../components/ThemeSelector'
import { ThemeProvider } from './ThemeProvider'
import { THEME_STORAGE_KEY } from './theme'

const mediaListeners = new Set<(event: MediaQueryListEvent) => void>()
let systemPrefersDark = false

beforeEach(() => {
    systemPrefersDark = false
    mediaListeners.clear()
    vi.stubGlobal('matchMedia', vi.fn().mockImplementation((query: string) => ({
        matches: systemPrefersDark,
        media: query,
        onchange: null,
        addEventListener: (_type: string, listener: (event: MediaQueryListEvent) => void) => {
            mediaListeners.add(listener)
        },
        removeEventListener: (_type: string, listener: (event: MediaQueryListEvent) => void) => {
            mediaListeners.delete(listener)
        },
        addListener: vi.fn(),
        removeListener: vi.fn(),
        dispatchEvent: vi.fn(),
    })))
})

function renderThemeSelector() {
    return render(
        <ThemeProvider>
            <ThemeSelector />
        </ThemeProvider>,
    )
}

describe('ThemeProvider', () => {
    it('저장된 선택이 없으면 시스템 다크 모드를 적용하고 변경을 추적한다', async () => {
        systemPrefersDark = true
        renderThemeSelector()

        expect(document.documentElement).toHaveAttribute('data-theme', 'dark')

        systemPrefersDark = false
        mediaListeners.forEach((listener) => listener({ matches: false } as MediaQueryListEvent))

        await waitFor(() => {
            expect(document.documentElement).toHaveAttribute('data-theme', 'light')
        })
    })

    it('저장된 라이트 모드 선택을 시스템 설정보다 우선한다', () => {
        systemPrefersDark = true
        localStorage.setItem(THEME_STORAGE_KEY, 'light')

        renderThemeSelector()

        expect(document.documentElement).toHaveAttribute('data-theme', 'light')
    })

    it('사용자가 선택한 다크 모드를 저장하고 즉시 적용한다', async () => {
        const user = userEvent.setup()
        renderThemeSelector()

        await user.click(screen.getByRole('button', { name: '테마 선택: 시스템 설정' }))
        await user.click(screen.getByRole('menuitemradio', { name: '다크 모드' }))

        expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark')
        expect(document.documentElement).toHaveAttribute('data-theme', 'dark')
    })
})
