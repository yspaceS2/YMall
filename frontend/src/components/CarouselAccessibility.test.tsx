import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { HomeEventCarousel } from './HomeEventCarousel'

describe('캐러셀 접근성', () => {
    beforeEach(() => {
        vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
            matches: true,
            addEventListener: vi.fn(),
            removeEventListener: vi.fn(),
        })))
    })

    it('모션 감소 설정에서는 자동 재생을 멈추고 사용자가 다시 재생할 수 있다', async () => {
        const user = userEvent.setup()

        render(
            <MemoryRouter>
                <HomeEventCarousel />
            </MemoryRouter>,
        )

        const playButton = screen.getByRole('button', { name: '이벤트 자동 재생' })
        expect(playButton).toHaveAttribute('aria-pressed', 'true')

        await user.click(playButton)

        expect(screen.getByRole('button', { name: '이벤트 자동 재생 일시 정지' })).toHaveAttribute('aria-pressed', 'false')
    })
})
