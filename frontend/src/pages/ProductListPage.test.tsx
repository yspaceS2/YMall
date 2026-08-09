import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getCategories, getProducts, getProductSuggestions } from '../api/products'
import type { Category, PageResponse, ProductSummary } from '../types/product'
import { ProductListPage } from './ProductListPage'

vi.mock('../api/products', () => ({
    getCategories: vi.fn(),
    getProducts: vi.fn(),
    getProductSuggestions: vi.fn(),
}))

vi.mock('../components/HomeEventCarousel', () => ({
    HomeEventCarousel: () => null,
}))

vi.mock('../components/HomeMerchandisingSections', () => ({
    HomeMerchandisingSections: () => null,
}))

const categories: Category[] = [
    {
        categoryId: 1,
        name: '패션',
        slug: 'fashion',
        parentId: null,
        depth: 1,
    },
    {
        categoryId: 2,
        name: '여성의류',
        slug: 'womens-clothing',
        parentId: 1,
        depth: 2,
    },
]

const emptyProducts: PageResponse<ProductSummary> = {
    content: [],
    page: 1,
    size: 12,
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
}

describe('ProductListPage 카테고리 검색', () => {
    beforeEach(() => {
        vi.mocked(getCategories).mockResolvedValue(categories)
        vi.mocked(getProducts).mockResolvedValue(emptyProducts)
        vi.mocked(getProductSuggestions).mockResolvedValue([
            {
                productId: 10,
                name: '데일리 재킷',
                thumbnailUrl: null,
                matchType: 'CONTAINS',
            },
        ])
    })

    it('선택한 카테고리 안에서 추천어를 조회하고 검색 조건을 유지한다', async () => {
        const user = userEvent.setup()

        render(
            <MemoryRouter initialEntries={['/?categoryId=2']}>
                <Routes>
                    <Route path="/" element={<ProductListPage />} />
                </Routes>
            </MemoryRouter>,
        )

        await waitFor(() => {
            expect(getProducts).toHaveBeenCalledWith(expect.objectContaining({
                categoryId: 2,
                page: 1,
                size: 12,
            }))
        })

        const searchInput = await screen.findByRole('combobox')
        await user.type(searchInput, '재킷')

        await waitFor(() => {
            expect(getProductSuggestions).toHaveBeenCalledWith(
                '재킷',
                8,
                2,
                expect.any(AbortSignal),
            )
        })

        await user.click(await screen.findByRole('option', { name: '데일리 재킷' }))

        await waitFor(() => {
            expect(getProducts).toHaveBeenLastCalledWith(expect.objectContaining({
                categoryId: 2,
                keyword: '데일리 재킷',
                page: 1,
                size: 12,
            }))
        })
    })
})
