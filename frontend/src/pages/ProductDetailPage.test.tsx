import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getProduct } from '../api/products'
import { getProductReviews, getProductReviewSummary } from '../api/reviews'
import { ProductDetailPage } from './ProductDetailPage'

vi.mock('../api/cart', () => ({
    addCartItem: vi.fn(),
}))

vi.mock('../api/products', () => ({
    getProduct: vi.fn(),
}))

vi.mock('../api/reviews', () => ({
    getProductReviews: vi.fn(),
    getProductReviewSummary: vi.fn(),
}))

vi.mock('../auth/useAuth', () => ({
    useAuth: () => ({ isAuthenticated: true }),
}))

vi.mock('../components/wishlist/ProductWishlistButton', () => ({
    ProductWishlistButton: () => <button type="button">찜하기</button>,
}))

vi.mock('../components/review/ReviewSummaryPanel', () => ({
    ReviewSummaryPanel: () => <div>AI 리뷰 요약</div>,
}))

const product = {
    productId: 1,
    category: { categoryId: 1, name: '패션', slug: 'fashion' },
    name: '테스트 상품',
    description: '테스트 상품 설명',
    brand: 'YMALL',
    price: 10_000,
    discountPercentage: 10,
    rating: 4.5,
    stock: 5,
    thumbnailUrl: 'https://example.com/thumbnail.jpg',
    status: 'APPROVED' as const,
    images: [
        {
            imageId: 1,
            originalUrl: 'https://example.com/gallery-01.jpg',
            imageUrl: 'https://example.com/gallery-01.jpg',
            sortOrder: 0,
        },
        {
            imageId: 2,
            originalUrl: 'https://example.com/gallery-02.jpg',
            imageUrl: 'https://example.com/gallery-02.jpg',
            sortOrder: 1,
        },
    ],
    detailImages: [
        {
            detailImageId: 11,
            originalUrl: 'https://example.com/detail-01.jpg',
            imageUrl: 'https://example.com/detail-01.jpg',
            sortOrder: 0,
        },
        {
            detailImageId: 12,
            originalUrl: 'https://example.com/detail-02.jpg',
            imageUrl: 'https://example.com/detail-02.jpg',
            sortOrder: 1,
        },
    ],
}

describe('ProductDetailPage', () => {
    beforeEach(() => {
        vi.mocked(getProduct).mockResolvedValue(product)
        vi.mocked(getProductReviews).mockResolvedValue({
            content: [],
            page: 1,
            size: 10,
            totalElements: 0,
            totalPages: 0,
            hasNext: false,
            hasPrevious: false,
        })
        vi.mocked(getProductReviewSummary).mockResolvedValue({
            available: false,
            reviewCount: 0,
            pros: [],
            cons: [],
            commonOpinions: [],
            modelVersion: null,
            generatedAt: null,
        })
    })

    it('갤러리 이미지를 선택하고 상세 이미지를 입력 순서대로 표시한다', async () => {
        const user = userEvent.setup()

        renderPage()

        const mainImage = await screen.findByRole('img', { name: '테스트 상품' })
        expect(mainImage).toHaveAttribute('src', 'https://example.com/gallery-01.jpg')

        await user.click(screen.getByRole('button', { name: '2번 상품 이미지 보기' }))
        expect(mainImage).toHaveAttribute('src', 'https://example.com/gallery-02.jpg')

        const detailImages = screen.getAllByRole('img', { name: /테스트 상품 상세 이미지/ })
        expect(detailImages).toHaveLength(2)
        expect(detailImages[0]).toHaveAttribute('src', 'https://example.com/detail-01.jpg')
        expect(detailImages[1]).toHaveAttribute('src', 'https://example.com/detail-02.jpg')
    })

    it('상품정보, 리뷰, Q&A 탭을 전환한다', async () => {
        const user = userEvent.setup()

        renderPage()

        expect(await screen.findByRole('tabpanel', { name: '상품정보' })).toBeInTheDocument()

        await user.click(screen.getByRole('tab', { name: /리뷰/ }))
        expect(screen.getByRole('tabpanel', { name: /리뷰/ })).toBeInTheDocument()
        expect(screen.getByText('AI 리뷰 요약')).toBeInTheDocument()

        await user.click(screen.getByRole('tab', { name: 'Q&A' }))
        expect(screen.getByRole('tabpanel', { name: 'Q&A' })).toBeInTheDocument()
        expect(screen.getByText('등록된 상품 문의가 없습니다')).toBeInTheDocument()

        await waitFor(() => expect(getProduct).toHaveBeenCalledWith(1, expect.any(AbortSignal)))
    })
})

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/products/1']}>
            <Routes>
                <Route path="/products/:productId" element={<ProductDetailPage />} />
            </Routes>
        </MemoryRouter>,
    )
}
