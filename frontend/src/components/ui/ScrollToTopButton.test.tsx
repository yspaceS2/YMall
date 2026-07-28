import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ScrollToTopButton } from './ScrollToTopButton'

describe('ScrollToTopButton', () => {
    afterEach(() => {
        Object.defineProperty(window, 'scrollY', { configurable: true, value: 0 })
        vi.restoreAllMocks()
    })

    it('스크롤한 뒤 나타나고 클릭하면 화면 최상단으로 이동한다', async () => {
        const user = userEvent.setup()
        const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => undefined)

        render(<ScrollToTopButton />)

        const button = screen.getByRole('button', { name: '맨 위로 이동' })
        expect(button).toHaveClass('invisible')

        Object.defineProperty(window, 'scrollY', { configurable: true, value: 500 })
        fireEvent.scroll(window)

        expect(button).toHaveClass('visible')

        await user.click(button)

        expect(scrollTo).toHaveBeenCalledWith({ top: 0, behavior: 'smooth' })
    })
})
