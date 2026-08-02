import { expect, test, type Page } from '@playwright/test'
import { installMockApi } from './fixtures/mockApi'

async function expectCarouselToWrap(
    page: Page,
    label: string,
    controlLabel: string,
    total: number,
    padded: boolean,
) {
    const carousel = page.getByRole('region', { name: label })
    const pauseButton = carousel.getByRole('button', { name: `${controlLabel} 자동 재생 일시 정지` })
    const nextButton = carousel.getByRole('button', { name: `다음 ${controlLabel}` })
    const counter = (index: number) => padded
        ? `${String(index).padStart(2, '0')} / ${String(total).padStart(2, '0')}`
        : `${index} / ${total}`

    await pauseButton.click()
    for (let index = 2; index <= total; index += 1) {
        await nextButton.click()
        await expect(carousel.getByText(counter(index), { exact: true })).toBeVisible()
    }

    await nextButton.click()
    await expect(carousel.getByText(counter(1), { exact: true })).toBeVisible()
}

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

    const grocery = page.getByRole('region', { name: '오늘의 장보기' })
    await expect(grocery.getByText('01 / 04')).toBeVisible()
    await expect(grocery.getByRole('heading', { name: '테스트 신선식품 베스트' })).toBeVisible()

    await grocery.getByRole('button', { name: '간편식 보기' }).click()
    await expect(grocery.getByText('04 / 04')).toBeVisible()
    await expect(grocery.getByRole('heading', { name: '테스트 간편식 추천' })).toBeVisible()

    await page.getByRole('button', { name: /테마 선택:/ }).click()
    await page.getByRole('menuitemradio', { name: '다크 모드' }).click()

    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
    await expect(page.getByRole('region', { name: '새로 들어온 상품' })).toBeVisible()
})

for (const carousel of [
    { label: '이벤트 프로모션', controlLabel: '이벤트', total: 8, padded: false },
    { label: '카테고리 베스트', controlLabel: '카테고리 베스트', total: 4, padded: true },
    { label: '오늘의 장보기', controlLabel: '오늘의 장보기', total: 4, padded: true },
    { label: '패션 에디트', controlLabel: '패션 에디트', total: 4, padded: true },
    { label: '새로 들어온 상품', controlLabel: '새로 들어온 상품', total: 4, padded: true },
] as const) {
    test(`${carousel.label} 캐러셀이 마지막 슬라이드에서 처음으로 순환한다`, async ({ page }) => {
        await installMockApi(page)
        await page.goto('/')

        await expectCarouselToWrap(
            page,
            carousel.label,
            carousel.controlLabel,
            carousel.total,
            carousel.padded,
        )
    })
}

test('메인 큐레이션 상품을 선택하면 상품 상세로 이동한다', async ({ page }) => {
    await installMockApi(page)
    await page.goto('/')

    const categoryBest = page.getByRole('region', { name: '카테고리 베스트' })
    await categoryBest.getByRole('link', { name: /테스트 무선 키보드/ }).click()

    await expect(page).toHaveURL('/products/1')
    await expect(page.getByRole('heading', { name: '테스트 무선 키보드' })).toBeVisible()
})
