import { expect, test } from '@playwright/test'
import { installMockApi, loginThroughUi } from './fixtures/mockApi'

test('seller reauthentication restores an authorized seller path', async ({ page }) => {
    await installMockApi(page)
    await page.goto('/seller/orders')

    await expect(page).toHaveURL(/\/login$/)
    await page.locator('#login-email').fill('seller@example.test')
    await page.locator('#login-password').fill('Test1234!')
    await page.locator('form:has(#login-email) button[type="submit"]').click()

    await expect(page).toHaveURL(/\/seller\/orders$/)
})

test('user login does not restore a seller path', async ({ page }) => {
    await installMockApi(page)
    await page.goto('/seller/orders')

    await expect(page).toHaveURL(/\/login$/)
    await page.locator('#login-email').fill('member@example.test')
    await page.locator('#login-password').fill('Test1234!')
    await page.locator('form:has(#login-email) button[type="submit"]').click()

    await expect(page).toHaveURL('/')
})

test('explicit logout clears the previous seller path before account switching', async ({ page }) => {
    await installMockApi(page)
    await loginThroughUi(page, 'seller@example.test')
    await page.goto('/seller')

    await page.locator('aside button').last().click()
    await expect(page).toHaveURL('/')

    await loginThroughUi(page, 'member@example.test')
    await expect(page).toHaveURL('/')
})
