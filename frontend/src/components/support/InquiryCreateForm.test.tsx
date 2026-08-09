import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { InquiryCreateForm } from './InquiryCreateForm'

describe('InquiryCreateForm', () => {
    it('판매자 문의의 제목과 내용을 정리해 제출한다', async () => {
        const user = userEvent.setup()
        const onSubmit = vi.fn().mockResolvedValue(undefined)

        render(
            <InquiryCreateForm
                seller
                submitting={false}
                onCancel={vi.fn()}
                onSubmit={onSubmit}
            />,
        )

        expect(screen.getByRole('combobox', { name: '문의 유형' }))
            .toHaveValue('PRODUCT_APPROVAL')
        await user.type(screen.getByRole('textbox', { name: '제목' }), '  상품 승인 문의  ')
        await user.type(
            screen.getByPlaceholderText('문의 내용을 자세히 적어 주세요'),
            '  확인 부탁드립니다.  ',
        )
        await user.click(screen.getByRole('button', { name: '문의 등록' }))

        await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({
            category: 'PRODUCT_APPROVAL',
            title: '상품 승인 문의',
            content: '확인 부탁드립니다.',
        }))
    })
})
