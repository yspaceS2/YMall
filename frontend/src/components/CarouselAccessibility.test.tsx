import { act, fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { HomeEventCarousel } from './HomeEventCarousel'

describe('캐러셀 접근성', () => {
    beforeEach(() => {
        vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
            matches: false,
            addEventListener: vi.fn(),
            removeEventListener: vi.fn(),
        })))
    })

    afterEach(() => {
        vi.useRealTimers()
        vi.unstubAllGlobals()
    })

    it('모션 감소 설정에서는 자동 재생을 멈추고 사용자가 다시 재생할 수 있다', async () => {
        vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
            matches: true,
            addEventListener: vi.fn(),
            removeEventListener: vi.fn(),
        })))
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

    it('자동 재생은 한 번씩 이동하고 수동 조작 후 타이머를 새로 시작한다', () => {
        vi.useFakeTimers()
        render(
            <MemoryRouter>
                <HomeEventCarousel />
            </MemoryRouter>,
        )

        act(() => vi.advanceTimersByTime(3_000))
        fireEvent.click(screen.getByRole('button', { name: '다음 이벤트' }))
        expect(screen.getByRole('heading', { name: /나를 위한\s+매일의 루틴/ })).toBeInTheDocument()

        act(() => vi.advanceTimersByTime(3_000))
        expect(screen.getByRole('heading', { name: /나를 위한\s+매일의 루틴/ })).toBeInTheDocument()

        act(() => vi.advanceTimersByTime(2_500))
        expect(screen.getByRole('heading', { name: /집 안에 더하는\s+작은 변화/ })).toBeInTheDocument()
    })

    it('마지막에서 첫 슬라이드로 갈 때도 같은 진행 방향을 유지한다', () => {
        render(
            <MemoryRouter>
                <HomeEventCarousel />
            </MemoryRouter>,
        )

        const nextButton = screen.getByRole('button', { name: '다음 이벤트' })
        fireEvent.click(nextButton)
        fireEvent.click(nextButton)
        fireEvent.click(nextButton)

        expect(screen.getByRole('heading', { name: /새로운 계절,\s+가벼운 옷차림/ })).toBeInTheDocument()
        expect(screen.getByRole('region', { name: '이벤트 프로모션' }).querySelector('[data-carousel-direction]')).toHaveAttribute('data-carousel-direction', 'forward')
    })

    it('호버와 키보드 포커스 중에는 자동 재생을 멈춘다', () => {
        vi.useFakeTimers()
        render(
            <MemoryRouter>
                <HomeEventCarousel />
            </MemoryRouter>,
        )

        const carousel = screen.getByRole('region', { name: '이벤트 프로모션' })
        fireEvent.mouseEnter(carousel)
        act(() => vi.advanceTimersByTime(6_000))
        expect(screen.getByRole('heading', { name: /새로운 계절,\s+가벼운 옷차림/ })).toBeInTheDocument()

        fireEvent.mouseLeave(carousel)
        const nextButton = screen.getByRole('button', { name: '다음 이벤트' })
        fireEvent.focus(nextButton)
        act(() => vi.advanceTimersByTime(6_000))
        expect(screen.getByRole('heading', { name: /새로운 계절,\s+가벼운 옷차림/ })).toBeInTheDocument()
    })

    it('모바일에서 왼쪽으로 스와이프하면 다음 슬라이드로 이동한다', () => {
        render(
            <MemoryRouter>
                <HomeEventCarousel />
            </MemoryRouter>,
        )

        const carousel = screen.getByRole('region', { name: '이벤트 프로모션' })
        fireEvent.touchStart(carousel, { touches: [{ clientX: 180 }] })
        fireEvent.touchEnd(carousel, { changedTouches: [{ clientX: 80 }] })

        expect(screen.getByRole('heading', { name: /나를 위한\s+매일의 루틴/ })).toBeInTheDocument()
    })
})
