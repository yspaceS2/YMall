import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Order } from '../types/order'
import { ToastProvider } from '../toast/ToastProvider'
import { OrderDetailPage } from './OrderDetailPage'

const mocks = vi.hoisted(() => ({
    createReturnRequest: vi.fn(),
    createReview: vi.fn(),
    deleteReview: vi.fn(),
    getAllMyReviews: vi.fn(),
    getOrder: vi.fn(),
    getRefunds: vi.fn(),
    getReturnRequests: vi.fn(),
    requestRefund: vi.fn(),
    updateReview: vi.fn(),
}))

vi.mock('../api/orders', () => ({
    createReturnRequest: mocks.createReturnRequest,
    getOrder: mocks.getOrder,
    getRefunds: mocks.getRefunds,
    getReturnRequests: mocks.getReturnRequests,
    requestRefund: mocks.requestRefund,
}))

vi.mock('../api/reviews', () => ({
    createReview: mocks.createReview,
    deleteReview: mocks.deleteReview,
    getAllMyReviews: mocks.getAllMyReviews,
    updateReview: mocks.updateReview,
}))

const deliveredOrder: Order = {
    orderId: 101,
    paymentOrderId: 'order-101',
    status: 'DELIVERED',
    totalAmount: 61_500,
    productAmount: 59_000,
    shippingFee: 2_500,
    refundSupported: true,
    createdAt: '2026-08-05T10:00:00',
    deliveryAddress: {
        recipientName: '테스트 구매자',
        recipientPhone: '01012345678',
        postalCode: '12345',
        roadAddress: '서울시 테스트로 1',
        detailAddress: '101호',
    },
    items: [{
        orderItemId: 1001,
        productId: 10,
        productName: '테스트 스니커즈',
        thumbnailUrl: '/images/product.jpg',
        unitPrice: 59_000,
        quantity: 1,
        refundedQuantity: 0,
        totalPrice: 59_000,
        shippingFee: 2_500,
        fulfillmentStatus: 'DELIVERED',
    }],
}

describe('OrderDetailPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.getOrder.mockResolvedValue(deliveredOrder)
        mocks.getAllMyReviews.mockResolvedValue([{
            reviewId: 501,
            orderItemId: 1001,
            productId: 10,
            authorName: '테스트 구매자',
            rating: 4,
            content: '착화감이 편안합니다.',
            createdAt: '2026-08-05T11:00:00',
            updatedAt: '2026-08-05T11:00:00',
        }])
        mocks.getReturnRequests.mockResolvedValue([{
            returnRequestId: 701,
            orderId: 101,
            orderItemId: 1001,
            productId: 10,
            productName: '테스트 스니커즈',
            thumbnailUrl: '/images/product.jpg',
            memberName: '테스트 구매자',
            quantity: 1,
            reason: '사이즈가 맞지 않습니다.',
            status: 'REJECTED',
            sellerResponse: '사용 흔적을 확인했습니다.',
            paymentRefundId: null,
            returnDeadline: null,
            requestedAt: '2026-08-05T12:00:00',
            processedAt: '2026-08-05T13:00:00',
        }])
        mocks.getRefunds.mockResolvedValue([])
    })

    it('주문 상품, 리뷰, 반품 상태와 결제·배송 정보를 표시한다', async () => {
        renderPage('/mypage/orders/101')

        expect(await screen.findByRole('heading', { name: '테스트 스니커즈' }))
            .toBeInTheDocument()
        expect(screen.getByText('착화감이 편안합니다.')).toBeInTheDocument()
        expect(screen.getByText('반품 거절')).toBeInTheDocument()
        expect(screen.getByText(/사용 흔적을 확인했습니다/)).toBeInTheDocument()
        expect(screen.getByText('테스트 구매자')).toBeInTheDocument()
        expect(screen.getByText('61,500원')).toBeInTheDocument()
    })

    it('리뷰 작성 내용을 저장하고 화면에 반영한다', async () => {
        const user = userEvent.setup()
        mocks.getAllMyReviews.mockResolvedValue([])
        mocks.createReview.mockResolvedValue({
            reviewId: 502,
            orderItemId: 1001,
            productId: 10,
            authorName: '테스트 구매자',
            rating: 5,
            content: '새 리뷰입니다.',
            createdAt: '2026-08-05T14:00:00',
            updatedAt: '2026-08-05T14:00:00',
        })
        renderPage('/mypage/orders/101')

        await user.click(await screen.findByRole('button', { name: '리뷰 작성' }))
        await user.type(
            screen.getByPlaceholderText('상품을 사용한 경험을 작성해 주세요.'),
            '새 리뷰입니다.',
        )
        await user.click(screen.getByRole('button', { name: '저장' }))

        await waitFor(() => {
            expect(mocks.createReview).toHaveBeenCalledWith({
                orderItemId: 1001,
                rating: 5,
                content: '새 리뷰입니다.',
            })
        })
        expect(await screen.findByText('새 리뷰입니다.')).toBeInTheDocument()
    })

    it('결제 완료 주문에서 환불 내역을 조회해 다이얼로그를 연다', async () => {
        const user = userEvent.setup()
        mocks.getOrder.mockResolvedValue({
            ...deliveredOrder,
            status: 'PAID',
            items: [{
                ...deliveredOrder.items[0],
                fulfillmentStatus: 'PENDING',
            }],
        })
        renderPage('/mypage/orders/101')

        await user.click(await screen.findByRole('button', { name: '환불 신청·내역' }))

        await waitFor(() => expect(mocks.getRefunds).toHaveBeenCalledWith(101))
        expect(screen.getByRole('heading', { name: '환불 신청 및 내역' }))
            .toBeInTheDocument()
    })

    it('잘못된 주문 주소에서는 API를 호출하지 않는다', async () => {
        renderPage('/mypage/orders/invalid')

        expect(await screen.findByText('올바르지 않은 주문 주소입니다.'))
            .toBeInTheDocument()
        expect(mocks.getOrder).not.toHaveBeenCalled()
    })
})

function renderPage(path: string) {
    return render(
        <MemoryRouter initialEntries={[path]}>
            <ToastProvider>
                <Routes>
                    <Route path="/mypage/orders/:orderId" element={<OrderDetailPage />} />
                </Routes>
            </ToastProvider>
        </MemoryRouter>,
    )
}
