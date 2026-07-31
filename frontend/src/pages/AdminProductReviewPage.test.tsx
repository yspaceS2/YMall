import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AdminProduct } from '../types/admin'
import { AdminProductReviewDetailPage } from './AdminProductReviewPage'

const mocks = vi.hoisted(() => ({
    getAdminProduct: vi.fn(),
    getAdminProducts: vi.fn(),
    updateAdminProductStatus: vi.fn(),
}))

vi.mock('../api/admin', () => ({
    getAdminProduct: mocks.getAdminProduct,
    getAdminProducts: mocks.getAdminProducts,
    updateAdminProductStatus: mocks.updateAdminProductStatus,
}))

const product: AdminProduct = {
    productId: 10,
    sellerProfileId: 2,
    storeName: 'YMall Seller',
    categoryName: '운동화',
    name: '검수 대상 스니커즈',
    description: '가볍고 편안한 데일리 스니커즈',
    brand: 'YMall',
    price: 59000,
    discountPercentage: 10,
    discountStartDate: '2026-07-01',
    discountEndDate: '2026-08-01',
    stock: 20,
    thumbnailUrl: '/images/product.jpg',
    freeShipping: true,
    shippingFee: 0,
    estimatedDeliveryDays: 3,
    images: [],
    detailImages: [],
    status: 'PENDING',
    rejectionReason: null,
    createdAt: '2026-07-31T10:00:00',
}

function renderDetailPage() {
    return render(
        <MemoryRouter initialEntries={['/admin/products/10']}>
            <Routes>
                <Route
                    path="/admin/products/:productId"
                    element={<AdminProductReviewDetailPage />}
                />
            </Routes>
        </MemoryRouter>,
    )
}

describe('AdminProductReviewDetailPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.getAdminProduct.mockResolvedValue(product)
        mocks.updateAdminProductStatus.mockResolvedValue({
            ...product,
            status: 'REJECTED',
            rejectionReason: '상세 설명을 보완해 주세요.',
        })
    })

    it('반려 사유를 필수로 받고 상품 반려 요청에 전달한다', async () => {
        const user = userEvent.setup()
        renderDetailPage()

        expect(await screen.findByText('검수 대상 스니커즈')).toBeInTheDocument()
        await user.click(screen.getByRole('button', { name: '반려' }))
        expect(screen.getByText('반려 사유를 입력해 주세요.')).toBeInTheDocument()
        expect(mocks.updateAdminProductStatus).not.toHaveBeenCalled()

        await user.type(
            screen.getByRole('textbox', { name: '반려 사유' }),
            '상세 설명을 보완해 주세요.',
        )
        await user.click(screen.getByRole('button', { name: '반려' }))

        await waitFor(() => {
            expect(mocks.updateAdminProductStatus).toHaveBeenCalledWith(
                10,
                'REJECTED',
                '상세 설명을 보완해 주세요.',
            )
        })
        expect(await screen.findByText(/판매자에게 전달/)).toBeInTheDocument()
    })
})
