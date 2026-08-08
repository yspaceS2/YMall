import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getCategories } from '../api/products'
import {
    createSellerProduct,
    getSellerProduct,
    getSellerProfile,
    updateSellerProduct,
} from '../api/seller'
import type { Category, ProductDetail } from '../types/product'
import { SellerProductEditorPage } from './SellerProductEditorPage'

const showToast = vi.fn()

vi.mock('../toast/useToast', () => ({
    useToast: () => ({ showToast }),
}))

vi.mock('../api/files', () => ({
    uploadProductImage: vi.fn(),
}))

vi.mock('../api/products', () => ({
    getCategories: vi.fn(),
}))

vi.mock('../api/seller', () => ({
    createSellerProduct: vi.fn(),
    getSellerProduct: vi.fn(),
    getSellerProfile: vi.fn(),
    updateSellerProduct: vi.fn(),
}))

const categories: Category[] = [
    { categoryId: 1, name: '패션', slug: 'fashion', parentId: null, depth: 1 },
    { categoryId: 2, name: '여성의류', slug: 'womens-clothing', parentId: 1, depth: 2 },
    { categoryId: 3, name: '원피스', slug: 'dresses', parentId: 2, depth: 3 },
]

const createdProduct: ProductDetail = {
    productId: 10,
    category: categories[2],
    name: '여름 원피스',
    description: '',
    brand: 'YMall',
    price: 39000,
    discountPercentage: 0,
    discountStartDate: null,
    discountEndDate: null,
    rating: null,
    stock: 7,
    thumbnailUrl: null,
    status: 'PENDING',
    freeShipping: true,
    shippingFee: 0,
    estimatedDeliveryDays: 3,
    images: [],
    detailImages: [],
}

describe('SellerProductEditorPage', () => {
    beforeEach(() => {
        vi.mocked(getSellerProfile).mockResolvedValue({
            sellerProfileId: 1,
            memberId: 2,
            storeName: '테스트 상점',
            businessNumber: '123-45-67890',
            description: null,
            createdAt: '2026-08-08T00:00:00',
            updatedAt: '2026-08-08T00:00:00',
        })
        vi.mocked(getCategories).mockResolvedValue(categories)
        vi.mocked(createSellerProduct).mockResolvedValue(createdProduct)
        vi.mocked(getSellerProduct).mockReset()
        vi.mocked(updateSellerProduct).mockReset()
        showToast.mockReset()
    })

    it('첫 소분류를 기본 선택하고 입력한 상품을 등록한다', async () => {
        const user = userEvent.setup()
        render(<SellerProductEditorPage />)

        expect(await screen.findByRole('heading', { name: '상품 관리' })).toBeInTheDocument()
        expect(screen.getByRole('combobox', { name: '소분류' })).toHaveValue('3')

        await user.type(screen.getByRole('textbox', { name: '상품명' }), '여름 원피스')
        await user.type(screen.getByRole('textbox', { name: '브랜드' }), 'YMall')
        await user.clear(screen.getByRole('spinbutton', { name: '가격' }))
        await user.type(screen.getByRole('spinbutton', { name: '가격' }), '39000')
        await user.clear(screen.getByRole('spinbutton', { name: '재고' }))
        await user.type(screen.getByRole('spinbutton', { name: '재고' }), '7')
        await user.click(screen.getByRole('button', { name: '상품 등록' }))

        await waitFor(() => expect(createSellerProduct).toHaveBeenCalledWith(
            expect.objectContaining({
                categoryId: 3,
                name: '여름 원피스',
                brand: 'YMall',
                price: 39000,
                stock: 7,
            }),
        ))
        expect(showToast).toHaveBeenCalledWith(
            '상품이 등록되었으며 승인을 기다립니다.',
            'success',
        )
    })
})
