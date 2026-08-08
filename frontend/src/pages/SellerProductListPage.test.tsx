import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getCategories } from '../api/products'
import { deleteSellerProduct, getSellerProducts } from '../api/seller'
import type { Category } from '../types/product'
import type { SellerProductSummary } from '../types/seller'
import { SellerProductListPage } from './SellerProductListPage'

vi.mock('../api/products', () => ({
    getCategories: vi.fn(),
}))

vi.mock('../api/seller', () => ({
    deleteSellerProduct: vi.fn(),
    getSellerProducts: vi.fn(),
}))

const categories: Category[] = [
    { categoryId: 1, name: '패션', slug: 'fashion', parentId: null, depth: 1 },
    { categoryId: 2, name: '여성의류', slug: 'womens-clothing', parentId: 1, depth: 2 },
    { categoryId: 3, name: '원피스', slug: 'dresses', parentId: 2, depth: 3 },
]

const product: SellerProductSummary = {
    productId: 10,
    categoryId: 3,
    categoryName: '원피스',
    name: '테스트 원피스',
    brand: 'YMall',
    price: 39000,
    discountPercentage: 0,
    rating: null,
    stock: 7,
    thumbnailUrl: null,
    status: 'APPROVED',
    rejectionReason: null,
}

function renderPage(path = '/seller/products') {
    return render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path="/seller/products" element={<SellerProductListPage />} />
            </Routes>
        </MemoryRouter>,
    )
}

describe('SellerProductListPage', () => {
    beforeEach(() => {
        vi.mocked(getCategories).mockResolvedValue(categories)
        vi.mocked(getSellerProducts).mockResolvedValue({
            content: [product],
            page: 1,
            size: 20,
            totalElements: 1,
            totalPages: 1,
            hasNext: false,
            hasPrevious: false,
        })
        vi.mocked(deleteSellerProduct).mockResolvedValue(undefined)
    })

    it('URL의 3단계 카테고리와 재고 조건으로 상품을 조회한다', async () => {
        renderPage('/seller/products?rootCategoryId=1&middleCategoryId=2&categoryId=3&stockCondition=LTE&stockQuantity=10')

        expect(await screen.findByText('테스트 원피스')).toBeInTheDocument()
        expect(getSellerProducts).toHaveBeenCalledWith(expect.objectContaining({
            page: 1,
            size: 20,
            categoryId: 3,
            stockCondition: 'LTE',
            stockQuantity: 10,
            signal: expect.any(AbortSignal),
        }))
        expect(screen.getByRole('combobox', { name: '대분류' })).toHaveValue('1')
        expect(screen.getByRole('combobox', { name: '중분류' })).toHaveValue('2')
        expect(screen.getByRole('combobox', { name: '소분류' })).toHaveValue('3')
    })

    it('확인 후 상품을 삭제하고 목록 건수를 갱신한다', async () => {
        const user = userEvent.setup()
        renderPage()

        await user.click(await screen.findByRole('button', { name: '테스트 원피스 삭제' }))
        expect(screen.getByRole('alertdialog')).toHaveTextContent("'테스트 원피스' 상품을 삭제합니다.")

        await user.click(screen.getByRole('button', { name: '상품 삭제' }))

        await waitFor(() => expect(deleteSellerProduct).toHaveBeenCalledWith(10))
        expect(screen.queryByText('테스트 원피스')).not.toBeInTheDocument()
        expect(screen.getByText('등록 상품 0개')).toBeInTheDocument()
    })
})
