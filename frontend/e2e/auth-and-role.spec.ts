import { expect, test } from '@playwright/test'
import { installMockApi, loginThroughUi } from './fixtures/mockApi'

test('이메일 중복 확인 후 회원가입하고 로그인 화면으로 이동한다', async ({ page }) => {
    await installMockApi(page)
    await page.goto('/signup')

    await page.locator('#signup-email').fill('new-member@example.test')
    await page.getByRole('button', { name: '중복 확인' }).click()
    await expect(page.getByText('사용 가능한 이메일입니다.')).toBeVisible()

    await page.locator('#signup-name').fill('테스트 회원')
    await page.locator('#signup-phone').fill('01000000000')
    await page.locator('#signup-password').fill('Test1234!')
    await page.locator('#signup-passwordConfirmation').fill('Test1234!')
    await page.getByRole('button', { name: '회원가입' }).click()

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByText('회원가입이 완료되었습니다. 새 계정으로 로그인해 주세요.')).toBeVisible()
})

test('일반 사용자는 판매자와 관리자 메뉴를 볼 수 없다', async ({ page }) => {
    await installMockApi(page)
    await loginThroughUi(page)

    await expect(page.getByRole('link', { name: '판매자 관리' })).toHaveCount(0)
    await expect(page.getByRole('link', { name: '관리자 운영' })).toHaveCount(0)
})

test('판매자와 관리자는 역할에 맞는 메뉴를 볼 수 있다', async ({ page }) => {
    await installMockApi(page)
    await loginThroughUi(page, 'admin@example.test')

    await expect(page.getByRole('link', { name: '판매자 관리' })).toBeVisible()
    await expect(page.getByRole('link', { name: '관리자 운영' })).toBeVisible()
})
