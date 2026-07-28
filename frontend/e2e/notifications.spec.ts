import { expect, test } from '@playwright/test'
import { installMockApi, loginThroughUi } from './fixtures/mockApi'

test('알림 배지와 개별·전체 읽음 처리를 확인한다', async ({ page }) => {
    await installMockApi(page)
    await loginThroughUi(page)

    await page.getByRole('button', { name: '내 정보 메뉴' }).click()
    const notificationLink = page.getByRole('menuitem', { name: /^알림\s+2$/ })
    await expect(notificationLink).toBeVisible()
    await notificationLink.click()
    await expect(page.getByRole('heading', { name: '알림' })).toBeVisible()

    await page.getByRole('button', { name: /주문이 생성되었습니다/ }).click()
    await expect(page).toHaveURL(/\/orders\/9001\/result$/)

    await page.goto('/notifications')
    await page.getByRole('button', { name: '모두 읽음' }).click()
    await expect(page.getByText('모든 알림을 읽음 처리했습니다.')).toBeVisible()
    await expect(page.getByRole('button', { name: '모두 읽음' })).toBeDisabled()
})
