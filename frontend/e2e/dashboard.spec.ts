import { expect, test } from '@playwright/test'
import { installMockApi, loginThroughUi } from './fixtures/mockApi'

test('판매자 대시보드 통계를 시각화하고 기간을 전환한다', async ({ page }) => {
    await installMockApi(page)
    await loginThroughUi(page, 'seller@example.test')
    await page.goto('/seller')

    await expect(page.getByText('3,650,000원')).toBeVisible()
    await expect(page.getByRole('img', { name: '기간별 순매출 추이' })).toBeVisible()
    await expect(page.getByText('판매량 상위 상품')).toBeVisible()
    const sellerViewport = await page.evaluate(() => ({
        innerHeight: window.innerHeight,
        scrollHeight: document.documentElement.scrollHeight,
    }))
    expect(sellerViewport.scrollHeight).toBeLessThanOrEqual(sellerViewport.innerHeight)
    await page.screenshot({ path: 'test-results/dashboard-seller.png', fullPage: true })

    await page.getByRole('button', { name: '6개월' }).click()
    await expect(page.getByRole('button', { name: '6개월' })).toHaveAttribute('aria-pressed', 'true')
    await page.getByRole('link', { name: '주문 12건 관리 페이지로 이동' }).click()
    await expect(page).toHaveURL(/\/seller\/orders\?workType=ACTION_REQUIRED$/)
    await expect(page.getByLabel('배송 상태')).toHaveValue('ACTION_REQUIRED')

    await page.goto('/seller')
    await page.getByRole('link', { name: '처리 중 740,000원 정산 관리 페이지로 이동' }).click()
    await expect(page).toHaveURL(/\/seller\/settlement\?tab=history&workType=PROCESSING$/)
    await expect(page.getByLabel('처리 상태')).toHaveValue('PROCESSING')
})

test('관리자 대시보드 거래와 운영 대기 현황을 시각화한다', async ({ page }) => {
    await installMockApi(page)
    await loginThroughUi(page, 'admin@example.test')
    await page.goto('/admin')

    await expect(page.getByText('128,450,000원')).toBeVisible()
    await expect(page.getByText('신규 회원·판매자')).toBeVisible()
    await expect(page.getByText('카테고리별 거래액')).toBeVisible()
    await expect(page.getByText('0원', { exact: true })).toBeVisible()
    await expect(page.getByRole('heading', { name: '처리 대기 업무' })).toBeVisible()
    await page.getByLabel('7.27 · 거래액 7,840,000원 · 주문 192건 · 판매 264개').hover()
    await expect(page.getByRole('tooltip')).toContainText('7.27 · 거래액 7,840,000원')

    const adminViewport = await page.evaluate(() => ({
        innerHeight: window.innerHeight,
        scrollHeight: document.documentElement.scrollHeight,
    }))
    expect(adminViewport.scrollHeight).toBeLessThanOrEqual(adminViewport.innerHeight)
    await page.screenshot({ path: 'test-results/dashboard-admin.png', fullPage: true })

    await page.setViewportSize({ width: 1920, height: 910 })
    await page.getByRole('button', { name: /테마 선택:/ }).click()
    await page.getByRole('menuitemradio', { name: '다크 모드' }).click()
    const wideViewport = await page.evaluate(() => ({
        innerHeight: window.innerHeight,
        scrollHeight: document.documentElement.scrollHeight,
    }))
    expect(wideViewport.scrollHeight).toBeLessThanOrEqual(wideViewport.innerHeight)
    await page.screenshot({ path: 'test-results/dashboard-admin-wide.png', fullPage: true })
    await page.getByRole('link', { name: '정산 처리 8건 관리 페이지로 이동' }).click()
    await expect(page).toHaveURL(/\/admin\/settlement\?workType=ACTION_REQUIRED$/)
    await expect(page.getByLabel('처리 상태')).toHaveValue('ACTION_REQUIRED')
})
