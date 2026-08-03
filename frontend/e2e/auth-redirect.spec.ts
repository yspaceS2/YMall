import { expect, test } from '@playwright/test'
import { installMockApi, loginThroughUi } from './fixtures/mockApi'

test('판매자 재인증은 허용된 판매자 경로로 복귀한다', async ({ page }) => {
    await installMockApi(page)
    await page.goto('/seller/orders')

    await expect(page).toHaveURL(/\/login$/)
    await page.locator('#login-email').fill('seller@example.test')
    await page.locator('#login-password').fill('Test1234!')
    await page.locator('form:has(#login-email) button[type="submit"]').click()

    await expect(page).toHaveURL(/\/seller\/orders$/)
})

test('일반 회원 로그인은 판매자 경로로 복귀하지 않는다', async ({ page }) => {
    await installMockApi(page)
    await page.goto('/seller/orders')

    await expect(page).toHaveURL(/\/login$/)
    await page.locator('#login-email').fill('member@example.test')
    await page.locator('#login-password').fill('Test1234!')
    await page.locator('form:has(#login-email) button[type="submit"]').click()

    await expect(page).toHaveURL('/')
})

test('판매자 로그인은 관리자 경로로 복귀하지 않는다', async ({ page }) => {
    await installMockApi(page)
    await page.goto('/admin/settlement')

    await expect(page).toHaveURL(/\/login$/)
    await page.locator('#login-email').fill('seller@example.test')
    await page.locator('#login-password').fill('Test1234!')
    await page.locator('form:has(#login-email) button[type="submit"]').click()

    await expect(page).toHaveURL('/')
})

test('판매자 로그아웃 후 일반 회원으로 전환하면 이전 경로를 제거한다', async ({ page }) => {
    await installMockApi(page)
    await loginThroughUi(page, 'seller@example.test')
    await page.goto('/seller')

    await page.getByRole('button', { name: '로그아웃', exact: true }).click()
    await expect(page).toHaveURL('/')

    await loginThroughUi(page, 'member@example.test')
    await expect(page).toHaveURL('/')
})

test('관리자 로그아웃 후 판매자로 전환하면 이전 관리자 경로를 제거한다', async ({ page }) => {
    await installMockApi(page)
    await loginThroughUi(page, 'admin@example.test')
    await page.goto('/admin/settlement')

    await page.getByRole('button', { name: '로그아웃', exact: true }).click()
    await expect(page).toHaveURL('/')

    await loginThroughUi(page, 'seller@example.test')
    await expect(page).toHaveURL('/')

    await page.goto('/seller')
    await expect(page.getByRole('heading', { name: '대시보드' })).toBeVisible()
})

test('일반 회원은 마이페이지를 이용하고 역할별 관리 화면 접근은 차단된다', async ({ page }) => {
    await installMockApi(page)
    await loginThroughUi(page)

    await page.goto('/mypage')
    await expect(page.getByRole('heading', { name: '내 정보 관리' })).toBeVisible()

    await page.goto('/seller')
    await expect(page).toHaveURL('/forbidden')
    await expect(page.getByRole('heading', { name: '접근 권한이 없습니다' })).toBeVisible()
})
