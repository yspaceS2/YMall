import { expect, test } from '@playwright/test'
import { installMockApi } from './fixtures/mockApi'

test('모바일에서 이벤트 캐러셀을 스와이프해 다음 슬라이드로 이동한다', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 })
    await installMockApi(page)
    await page.goto('/')

    const carousel = page.getByRole('region', { name: '이벤트 프로모션' })
    await expect(carousel.getByRole('heading', { name: /새로운 계절,\s+가벼운 옷차림/ })).toBeVisible()

    await carousel.dispatchEvent('touchstart', {
        touches: [{ identifier: 1, clientX: 180, clientY: 200 }],
    })
    await carousel.dispatchEvent('touchend', {
        changedTouches: [{ identifier: 1, clientX: 80, clientY: 200 }],
    })

    await expect(carousel.getByRole('heading', { name: /나를 위한\s+매일의 루틴/ })).toBeVisible()
})
