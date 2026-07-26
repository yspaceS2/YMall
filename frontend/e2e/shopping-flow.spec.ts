import { expect, test } from '@playwright/test'
import { installMockApi, loginThroughUi } from './fixtures/mockApi'

test('상품을 장바구니에 담아 주문을 만들고 결제 화면까지 이동한다', async ({ page }) => {
    await installMockApi(page)
    await loginThroughUi(page)

    await page.getByRole('link', { name: '테스트 무선 키보드' }).first().click()
    await expect(page.getByRole('heading', { name: '테스트 무선 키보드' })).toBeVisible()

    await page.getByRole('button', { name: /장바구니 담기/ }).click()
    await expect(page).toHaveURL(/\/cart$/)
    await expect(page.getByText('테스트 무선 키보드')).toBeVisible()

    await page.getByRole('link', { name: '주문서 작성' }).click()
    await expect(page.getByRole('heading', { name: '주문서' })).toBeVisible()
    await page.getByRole('button', { name: '주문 생성 후 결제하기' }).click()

    await expect(page).toHaveURL(/\/orders\/9001\/payment$/)
    await expect(page.getByRole('heading', { name: '결제하기' })).toBeVisible()
    await expect(page.getByRole('button', { name: '45,000원 결제하기' })).toBeVisible()
})
