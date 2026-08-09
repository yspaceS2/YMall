import { render, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SellerProductQuestionDetailPage } from './SellerProductQuestionDetailPage'
import { SellerProductQuestionListPage } from './SellerProductQuestionsPage'
import { SellerReturnRequestDetailPage } from './SellerReturnRequestDetailPage'
import { SellerReturnRequestsPage } from './SellerReturnRequestsPage'

const sellerApiMocks = vi.hoisted(() => ({
    approveSellerReturnRequest: vi.fn(),
    getSellerReturnRequest: vi.fn(),
    getSellerReturnRequests: vi.fn(),
    rejectSellerReturnRequest: vi.fn(),
}))

const questionApiMocks = vi.hoisted(() => ({
    getSellerProductQuestion: vi.fn(),
    getSellerProductQuestions: vi.fn(),
    notifySellerQuestionCountChanged: vi.fn(),
    saveSellerProductQuestionAnswer: vi.fn(),
}))

vi.mock('../api/seller', () => sellerApiMocks)
vi.mock('../api/productQuestions', () => questionApiMocks)

function renderRoute(path: string, pattern: string, element: React.ReactNode) {
    render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path={pattern} element={element} />
            </Routes>
        </MemoryRouter>,
    )
}

describe('분리된 판매자 관리 페이지 경계', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        sellerApiMocks.getSellerReturnRequests.mockResolvedValue({
            content: [],
            page: 1,
            size: 20,
            totalElements: 0,
            totalPages: 0,
        })
        questionApiMocks.getSellerProductQuestions.mockResolvedValue({
            content: [],
            page: 1,
            size: 20,
            totalElements: 0,
            totalPages: 0,
        })
        sellerApiMocks.getSellerReturnRequest.mockReturnValue(new Promise(() => undefined))
        questionApiMocks.getSellerProductQuestion.mockReturnValue(new Promise(() => undefined))
    })

    it('반품 목록에서는 목록 API만 호출한다', async () => {
        renderRoute('/seller/returns', '/seller/returns', <SellerReturnRequestsPage />)

        await waitFor(() => expect(sellerApiMocks.getSellerReturnRequests).toHaveBeenCalledOnce())
        expect(sellerApiMocks.getSellerReturnRequest).not.toHaveBeenCalled()
    })

    it('반품 상세에서는 선택한 요청의 상세 API만 호출한다', async () => {
        renderRoute(
            '/seller/returns/7',
            '/seller/returns/:returnRequestId',
            <SellerReturnRequestDetailPage />,
        )

        await waitFor(() => expect(sellerApiMocks.getSellerReturnRequest).toHaveBeenCalledWith(
            7,
            expect.any(AbortSignal),
        ))
        expect(sellerApiMocks.getSellerReturnRequests).not.toHaveBeenCalled()
    })

    it('상품 문의 목록에서는 목록 API만 호출한다', async () => {
        renderRoute(
            '/seller/questions',
            '/seller/questions',
            <SellerProductQuestionListPage />,
        )

        await waitFor(() => expect(questionApiMocks.getSellerProductQuestions).toHaveBeenCalledOnce())
        expect(questionApiMocks.getSellerProductQuestion).not.toHaveBeenCalled()
    })

    it('상품 문의 상세에서는 선택한 문의의 상세 API만 호출한다', async () => {
        renderRoute(
            '/seller/questions/9',
            '/seller/questions/:questionId',
            <SellerProductQuestionDetailPage />,
        )

        await waitFor(() => expect(questionApiMocks.getSellerProductQuestion).toHaveBeenCalledWith(
            9,
            expect.any(AbortSignal),
        ))
        expect(questionApiMocks.getSellerProductQuestions).not.toHaveBeenCalled()
    })
})
