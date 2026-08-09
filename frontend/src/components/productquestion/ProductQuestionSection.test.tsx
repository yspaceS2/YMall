import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
    createProductQuestion,
    deleteProductQuestion,
    getProductQuestions,
    updateProductQuestion,
} from '../../api/productQuestions'
import type { ProductQuestion } from '../../types/productQuestion'
import { ProductQuestionSection } from './ProductQuestionSection'

vi.mock('../../api/productQuestions', () => ({
    createProductQuestion: vi.fn(),
    deleteProductQuestion: vi.fn(),
    getProductQuestions: vi.fn(),
    updateProductQuestion: vi.fn(),
}))

const question: ProductQuestion = {
    questionId: 1,
    productId: 10,
    productName: '테스트 상품',
    thumbnailUrl: null,
    memberName: '회원',
    title: '배송 문의',
    content: '언제 배송되나요?',
    privateQuestion: false,
    ownedByRequester: true,
    contentVisible: true,
    status: 'WAITING',
    answer: null,
    createdAt: '2026-08-08T10:00:00',
    updatedAt: '2026-08-08T10:00:00',
}

const page = (content: ProductQuestion[], hasNext = false) => ({
    content,
    page: 1,
    size: 10,
    totalElements: content.length,
    totalPages: hasNext ? 2 : 1,
    hasNext,
    hasPrevious: false,
})

function renderSection(isAuthenticated = true) {
    const onLoginRequired = vi.fn()
    const onSuccess = vi.fn()
    render(
        <ProductQuestionSection
            productId={10}
            isAuthenticated={isAuthenticated}
            onLoginRequired={onLoginRequired}
            onSuccess={onSuccess}
        />,
    )
    return { onLoginRequired, onSuccess }
}

describe('ProductQuestionSection', () => {
    beforeEach(() => {
        vi.mocked(getProductQuestions).mockResolvedValue(page([question]))
        vi.mocked(createProductQuestion).mockResolvedValue(question)
        vi.mocked(updateProductQuestion).mockResolvedValue(question)
        vi.mocked(deleteProductQuestion).mockResolvedValue(undefined)
    })

    it('비로그인 사용자가 문의를 시작하면 로그인을 요청한다', async () => {
        const user = userEvent.setup()
        const { onLoginRequired } = renderSection(false)

        await screen.findByText('배송 문의')
        await user.click(screen.getByRole('button', { name: '상품 문의하기' }))

        expect(onLoginRequired).toHaveBeenCalledOnce()
        expect(screen.queryByRole('textbox', { name: '문의 제목' })).not.toBeInTheDocument()
    })

    it('다음 페이지 문의를 기존 목록 뒤에 추가한다', async () => {
        const nextQuestion = { ...question, questionId: 2, title: '재입고 문의' }
        vi.mocked(getProductQuestions)
            .mockResolvedValueOnce({ ...page([question], true), totalElements: 2 })
            .mockResolvedValueOnce({ ...page([nextQuestion]), page: 2, hasPrevious: true })
        const user = userEvent.setup()
        renderSection()

        await user.click(await screen.findByRole('button', { name: '문의 더 보기' }))

        expect(await screen.findByText('재입고 문의')).toBeInTheDocument()
        expect(getProductQuestions).toHaveBeenLastCalledWith(10, 2, 10, expect.any(AbortSignal))
    })

    it('공백을 정리해 비밀 문의를 등록한다', async () => {
        const saved = { ...question, questionId: 3, title: '교환 문의', privateQuestion: true }
        vi.mocked(createProductQuestion).mockResolvedValue(saved)
        const user = userEvent.setup()
        const { onSuccess } = renderSection()

        await screen.findByText('배송 문의')
        await user.click(screen.getByRole('button', { name: '상품 문의하기' }))
        fireEvent.change(screen.getByRole('textbox', { name: '문의 제목' }), {
            target: { value: '  교환 문의  ' },
        })
        fireEvent.change(screen.getByRole('textbox', { name: '문의 내용' }), {
            target: { value: '  교환 가능한가요?  ' },
        })
        await user.click(screen.getByRole('checkbox', { name: '비밀 문의로 등록' }))
        await user.click(screen.getByRole('button', { name: '문의 등록' }))

        await waitFor(() => expect(createProductQuestion).toHaveBeenCalledWith(10, {
            title: '교환 문의',
            content: '교환 가능한가요?',
            privateQuestion: true,
        }))
        expect(onSuccess).toHaveBeenCalledWith('상품 문의가 등록되었습니다.')
    })

    it('작성자에게 수정과 삭제 동작을 제공한다', async () => {
        const user = userEvent.setup()
        renderSection()

        await user.click(await screen.findByRole('button', { name: '문의 수정' }))
        fireEvent.change(screen.getByRole('textbox', { name: '문의 제목' }), {
            target: { value: '배송 일정 문의' },
        })
        const form = screen.getByRole('textbox', { name: '문의 제목' }).closest('form')
        expect(form).not.toBeNull()
        await user.click(within(form as HTMLFormElement).getByRole('button', { name: '문의 수정' }))
        await waitFor(() => expect(updateProductQuestion).toHaveBeenCalledWith(
            1,
            expect.objectContaining({ title: '배송 일정 문의' }),
        ))

        await user.click(screen.getByRole('button', { name: '문의 삭제' }))
        await user.click(within(screen.getByRole('alertdialog')).getByRole('button', {
            name: '문의 삭제',
        }))
        await waitFor(() => expect(deleteProductQuestion).toHaveBeenCalledWith(1))
    })
})
