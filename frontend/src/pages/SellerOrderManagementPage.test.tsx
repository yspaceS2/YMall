import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { SellerOrderDetail } from '../types/seller'
import {
    SellerOrderDetailPage,
    SellerOrderListPage,
} from './SellerOrderManagementPage'

const mocks = vi.hoisted(() => ({
    getSellerOrder: vi.fn(),
    getSellerOrders: vi.fn(),
    getSellerRefunds: vi.fn(),
    requestSellerRefund: vi.fn(),
    updateSellerOrderItemFulfillment: vi.fn(),
}))

vi.mock('../api/seller', () => mocks)

const order: SellerOrderDetail = {
    orderId: 101,
    orderStatus: 'PREPARING',
    sellerAmount: 59000,
    createdAt: '2026-07-31T10:00:00',
    refundSupported: true,
    deliveryAddress: {
        recipientName: '테스트 구매자',
        recipientPhone: '01012345678',
        postalCode: '12345',
        roadAddress: '서울시 테스트로 1',
        detailAddress: '101호',
    },
    items: [
        {
            orderItemId: 1001,
            productId: 10,
            productName: '개별 출고 스니커즈',
            unitPrice: 59000,
            quantity: 1,
            refundedQuantity: 0,
            lineTotal: 59000,
            thumbnailUrl: '/images/product.jpg',
            fulfillmentStatus: 'PREPARING',
            carrier: null,
            trackingNumber: null,
            shippedAt: null,
            deliveredAt: null,
        },
    ],
}

function pageResponse() {
    return {
        content: [order],
        page: 1,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        hasNext: false,
        hasPrevious: false,
    }
}

describe('SellerOrderManagementPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.getSellerOrders.mockResolvedValue(pageResponse())
        mocks.getSellerOrder.mockResolvedValue(order)
        mocks.getSellerRefunds.mockResolvedValue([])
        mocks.updateSellerOrderItemFulfillment.mockResolvedValue({
            ...order,
            orderStatus: 'SHIPPED',
            items: [{
                ...order.items[0],
                fulfillmentStatus: 'SHIPPED',
                carrier: 'CJ대한통운',
                trackingNumber: '1234567890',
                shippedAt: '2026-07-31T11:00:00',
            }],
        })
    })

    it('배송 상태 필터를 판매자 주문 목록 요청에 반영한다', async () => {
        const user = userEvent.setup()
        render(
            <MemoryRouter>
                <SellerOrderListPage />
            </MemoryRouter>,
        )

        expect(await screen.findByText('개별 출고 스니커즈')).toBeInTheDocument()
        await user.selectOptions(screen.getByLabelText('배송 상태'), 'SHIPPED')

        await waitFor(() => {
            expect(mocks.getSellerOrders).toHaveBeenLastCalledWith(
                expect.objectContaining({ page: 1, fulfillmentStatus: 'SHIPPED' }),
            )
        })
    })

    it('상품별 운송장 정보를 입력해 배송 시작을 요청한다', async () => {
        const user = userEvent.setup()
        render(
            <MemoryRouter initialEntries={['/seller/orders/101']}>
                <Routes>
                    <Route
                        path="/seller/orders/:orderId"
                        element={<SellerOrderDetailPage />}
                    />
                </Routes>
            </MemoryRouter>,
        )

        expect(await screen.findByText('테스트 구매자')).toBeInTheDocument()
        await user.click(screen.getByRole('button', { name: '운송장 등록' }))
        await user.type(screen.getByLabelText('택배사'), 'CJ대한통운')
        await user.type(screen.getByLabelText('운송장 번호'), '1234567890')
        await user.click(screen.getByRole('button', { name: '배송 시작' }))

        await waitFor(() => {
            expect(mocks.updateSellerOrderItemFulfillment).toHaveBeenCalledWith(
                101,
                1001,
                {
                    fulfillmentStatus: 'SHIPPED',
                    carrier: 'CJ대한통운',
                    trackingNumber: '1234567890',
                },
            )
        })
    })
})
