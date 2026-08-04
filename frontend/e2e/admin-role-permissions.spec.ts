import { expect, test } from '@playwright/test'
import { installMockApi, loginThroughUi } from './fixtures/mockApi'

test.describe('관리자 등급별 화면 접근', () => {
    for (const adminGrade of ['MANAGER', 'SUPERVISOR'] as const) {
        test(`${adminGrade}는 조회 화면을 이용하지만 전체 카테고리 생성 화면에는 접근하지 못한다`, async ({ page }) => {
            await installMockApi(page, { role: 'ROLE_ADMIN', adminGrade })
            await loginThroughUi(page, 'admin@example.test')

            await page.goto('/admin/members')
            await expect(page).toHaveURL('/admin/members')

            await page.goto('/admin/categories/new')
            await expect(page).toHaveURL('/forbidden')
        })
    }

    test('Super Admin은 전체 카테고리 생성 화면에 접근할 수 있다', async ({ page }) => {
        await installMockApi(page, { role: 'ROLE_ADMIN', adminGrade: 'SUPER_ADMIN' })
        await loginThroughUi(page, 'admin@example.test')

        await page.goto('/admin/categories/new')

        await expect(page).toHaveURL('/admin/categories/new')
    })
})
