import { expect, test } from '@playwright/test'
import { installMockApi } from './fixtures/mockApi'

test('모바일에서 이벤트 캐러셀을 스와이프해 다음 슬라이드로 이동한다', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 })
    await installMockApi(page)
    await page.goto('/')

    const carousel = page.getByRole('region', { name: '이벤트 프로모션' })
    await expect(carousel.getByRole('heading', { name: /새로운 계절의\s+패션 컬렉션/ })).toBeVisible()

    await carousel.dispatchEvent('touchstart', {
        touches: [{ identifier: 1, clientX: 180, clientY: 200 }],
    })
    await carousel.dispatchEvent('touchend', {
        changedTouches: [{ identifier: 1, clientX: 80, clientY: 200 }],
    })

    await expect(carousel.getByRole('heading', { name: /매일을 채우는\s+뷰티 루틴/ })).toBeVisible()
})

test('모바일 홈 큐레이션을 전환하고 다크 테마에서도 표시한다', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 })
    await installMockApi(page)
    await page.goto('/')

    const categoryBest = page.getByRole('region', { name: '카테고리 베스트' })
    await expect(categoryBest.getByRole('heading', { name: '테스트 무선 키보드' })).toBeVisible()

    await categoryBest.getByRole('button', { name: '다음 카테고리 베스트' }).click()
    await expect(categoryBest.getByRole('heading', { name: '테스트 패션 재킷' })).toBeVisible()

    await page.getByRole('button', { name: /테마 선택:/ }).click()
    await page.getByRole('menuitemradio', { name: '다크 모드' }).click()

    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
    await expect(page.getByRole('region', { name: '새로 들어온 상품' })).toBeVisible()
})
