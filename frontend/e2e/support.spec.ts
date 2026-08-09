import { expect, test } from '@playwright/test'
import { installMockApi, loginThroughUi } from './fixtures/mockApi'

test('회원은 고객센터에서 문의를 등록하고 내역을 확인한다', async ({ page }) => {
    await installMockApi(page)
    await loginThroughUi(page)
    await page.goto('/mypage/support')

    await expect(page.getByRole('heading', { name: '고객센터' })).toBeVisible()
    await page.getByRole('button', { name: '새 문의' }).click()
    await page.getByLabel('제목', { exact: true }).fill('결제 내역 문의')
    await page.getByLabel('문의 내용').fill('결제 내역을 확인해 주세요.')
    await page.getByRole('button', { name: '문의 등록' }).click()

    await expect(page.getByRole('heading', { name: '결제 내역 문의' })).toBeVisible()
    await expect(page.getByText('결제 내역을 확인해 주세요.')).toBeVisible()
})

test('관리자는 답변 대기 문의를 필터링하고 처리 결과로 완료한다', async ({ page }) => {
    await installMockApi(page)
    await loginThroughUi(page, 'admin@example.test')
    await page.goto('/admin/support?status=WAITING')

    await expect(page.getByLabel('문의 상태')).toHaveValue('WAITING')
    await page.getByPlaceholder('제목, 요청자 또는 담당자 검색').fill('상담 관리자')
    await page.getByRole('button', { name: '검색' }).click()
    await page.getByRole('link', { name: /배송 상태를 확인해 주세요/ }).click()
    await expect(page.getByRole('heading', { name: '배송 상태를 확인해 주세요' })).toBeVisible()
    const scrollLayout = await page.evaluate(() => {
        const conversation = document.querySelector('[aria-label="문의 대화"]')
        return {
            pageFitsViewport: document.documentElement.scrollHeight <= window.innerHeight,
            conversationOverflowY: conversation
                ? window.getComputedStyle(conversation).overflowY
                : null,
        }
    })
    expect(scrollLayout).toEqual({
        pageFitsViewport: true,
        conversationOverflowY: 'auto',
    })
    const replyInput = page.getByPlaceholder('답변을 입력해 주세요')
    await replyInput.fill('배송 상태를 확인 중입니다.')
    const singleLineHeight = await replyInput.evaluate((element) => element.getBoundingClientRect().height)
    await replyInput.press('Shift+Enter')
    await expect(replyInput).toHaveValue('배송 상태를 확인 중입니다.\n')
    const multiLineHeight = await replyInput.evaluate((element) => element.getBoundingClientRect().height)
    expect(singleLineHeight).toBe(48)
    expect(multiLineHeight).toBeGreaterThan(singleLineHeight)
    await replyInput.fill('배송 상태를 확인 중입니다.')
    await replyInput.press('Enter')
    await expect(page.getByText('배송 상태를 확인 중입니다.')).toBeVisible()

    await page.getByPlaceholder('처리 결과를 작성하고 문의를 완료하세요').fill('배송 준비가 정상 반영되었습니다.')
    await page.getByRole('button', { name: '처리 완료' }).click()
    await expect(page.locator('span').filter({ hasText: /^처리 완료$/ })).toBeVisible()
    await expect(page.getByText('배송 준비가 정상 반영되었습니다.')).toBeVisible()
})
