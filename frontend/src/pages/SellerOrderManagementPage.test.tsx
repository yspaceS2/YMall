import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useNavigate } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { SellerOrderDetail } from '../types/seller'
import { SellerOrderDetailPage } from './SellerOrderDetailPage'
import { SellerOrderListPage } from './SellerOrderListPage'

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
        masked: false,
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

function OrderListWithKeywordNavigation() {
    const navigate = useNavigate()
    return (
        <>
            <button
                type="button"
                onClick={() => navigate('/seller/orders?keyword=두번째')}
            >
                검색 기록 이동
            </button>
            <SellerOrderListPage />
        </>
    )
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
        mocks.getSellerOrders.mockResolvedValue({
            ...pageResponse(),
            content: [{
                ...order,
                items: [
                    order.items[0],
                    {
                        ...order.items[0],
                        orderItemId: 1002,
                        productName: '아직 준비하지 않은 상품',
                        fulfillmentStatus: 'PENDING',
                    },
                ],
            }],
        })
        render(
            <MemoryRouter>
                <SellerOrderListPage />
            </MemoryRouter>,
        )

        expect(await screen.findByText('개별 출고 스니커즈 외 1개')).toBeInTheDocument()
        const orderTable = within(screen.getByRole('table'))
        expect(orderTable.getByText('상품 준비 중')).toBeInTheDocument()
        expect(orderTable.getByText('처리 대기')).toBeInTheDocument()
        await user.selectOptions(screen.getByLabelText('배송 상태'), 'SHIPPED')

        await waitFor(() => {
            expect(mocks.getSellerOrders).toHaveBeenLastCalledWith(
                expect.objectContaining({ page: 1, fulfillmentStatus: 'SHIPPED' }),
            )
        })
    })

    it('처리 필요 필터로 처리 대기와 상품 준비 중 주문을 함께 요청한다', async () => {
        render(
            <MemoryRouter initialEntries={['/seller/orders?workType=ACTION_REQUIRED']}>
                <SellerOrderListPage />
            </MemoryRouter>,
        )

        expect(await screen.findByLabelText('배송 상태')).toHaveValue('ACTION_REQUIRED')
        expect(mocks.getSellerOrders).toHaveBeenLastCalledWith(
            expect.objectContaining({
                fulfillmentStatus: '',
                workType: 'ACTION_REQUIRED',
            }),
        )
    })

    it('주문번호 또는 상품명 검색어를 주문 목록 요청에 반영한다', async () => {
        const user = userEvent.setup()
        render(
            <MemoryRouter>
                <SellerOrderListPage />
            </MemoryRouter>,
        )

        await screen.findByText('개별 출고 스니커즈')
        await user.type(
            screen.getByRole('textbox', { name: '검색어' }),
            '스니커즈',
        )
        await user.click(screen.getByRole('button', { name: '검색' }))

        await waitFor(() => {
            expect(mocks.getSellerOrders).toHaveBeenLastCalledWith(
                expect.objectContaining({
                    page: 1,
                    keyword: '스니커즈',
                }),
            )
        })
    })

    it('URL 검색어가 바뀌면 검색창 표시값도 동기화한다', async () => {
        const user = userEvent.setup()
        render(
            <MemoryRouter initialEntries={['/seller/orders?keyword=첫번째']}>
                <OrderListWithKeywordNavigation />
            </MemoryRouter>,
        )

        expect(await screen.findByRole('textbox', { name: '검색어' }))
            .toHaveValue('첫번째')
        await user.click(screen.getByRole('button', { name: '검색 기록 이동' }))

        await waitFor(() => {
            expect(screen.getByRole('textbox', { name: '검색어' }))
                .toHaveValue('두번째')
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

    it('보존 기간이 지난 배송 개인정보를 마스킹 안내로 표시한다', async () => {
        mocks.getSellerOrder.mockResolvedValue({
            ...order,
            deliveryAddress: {
                recipientName: '***',
                recipientPhone: '***',
                postalCode: '***',
                roadAddress: '***',
                detailAddress: null,
                masked: true,
            },
        })
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

        expect(await screen.findByText(
            '배송 완료 후 보존 기간이 지나 개인정보가 마스킹되었습니다.',
        )).toBeInTheDocument()
        expect(screen.queryByText('테스트 구매자')).not.toBeInTheDocument()
    })

    it('판매자 환불 다이얼로그를 판매 취소 업무 문구로 표시한다', async () => {
        const user = userEvent.setup()
        mocks.getSellerOrder.mockResolvedValue({
            ...order,
            items: [{
                ...order.items[0],
                fulfillmentStatus: 'PENDING',
            }],
        })
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

        await user.click(await screen.findByRole('button', { name: '판매 취소' }))

        expect(await screen.findByRole('heading', {
            name: '판매 취소 및 환불 처리',
        })).toBeInTheDocument()
        expect(screen.getByText('판매 취소할 상품')).toBeInTheDocument()
    })
})
