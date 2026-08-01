import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getHomeMerchandising } from '../api/home'
import type { HomeMerchandising } from '../types/home'
import { HomeEventCarousel } from './HomeEventCarousel'
import { HomeMerchandisingSections } from './HomeMerchandisingSections'

vi.mock('../api/home', () => ({
    getHomeMerchandising: vi.fn(),
}))

const mockedGetHomeMerchandising = vi.mocked(getHomeMerchandising)

const merchandising: HomeMerchandising = {
    categoryBest: [
        {
            categoryId: 1,
            categoryName: '패션',
            categorySlug: 'fashion',
            products: [{
                productId: 11,
                categoryId: 2,
                categoryName: '여성의류',
                name: '카테고리 베스트 재킷',
                brand: 'YMALL',
                price: 120_000,
                discountPercentage: 20,
                rating: 4.8,
                thumbnailUrl: '/uploads/jacket.jpg',
                salesQuantity: 30,
            }],
        },
        {
            categoryId: 10,
            categoryName: '뷰티',
            categorySlug: 'beauty',
            products: [{
                productId: 12,
                categoryId: 11,
                categoryName: '스킨케어',
                name: '카테고리 베스트 세럼',
                brand: 'GLOW',
                price: 35_000,
                discountPercentage: 0,
                rating: 4.6,
                thumbnailUrl: null,
                salesQuantity: 25,
            }],
        },
    ],
    grocery: [{
        categoryId: 21,
        categoryName: '신선식품',
        categorySlug: 'fresh-food',
        products: [{
            productId: 21,
            categoryId: 21,
            categoryName: '신선식품',
            name: '당일 수확 토마토',
            brand: 'FRESH',
            price: 15_000,
            discountPercentage: 10,
            rating: 4.9,
            thumbnailUrl: null,
            salesQuantity: 18,
        }],
    }],
    fashion: [{
        categoryId: 2,
        categoryName: '여성의류',
        categorySlug: 'women',
        products: [
            {
                productId: 31,
                categoryId: 2,
                categoryName: '여성의류',
                name: '시즌 트렌치코트',
                brand: 'YMALL',
                price: 159_000,
                discountPercentage: 15,
                rating: 4.7,
                thumbnailUrl: null,
                salesQuantity: 14,
            },
            {
                productId: 32,
                categoryId: 2,
                categoryName: '여성의류',
                name: '데일리 셔츠',
                brand: 'YMALL',
                price: 59_000,
                discountPercentage: 0,
                rating: 4.5,
                thumbnailUrl: null,
                salesQuantity: 12,
            },
        ],
    }],
    newArrivals: [{
        productId: 41,
        categoryId: 30,
        categoryName: '가전',
        name: '신상품 무선 스피커',
        brand: 'SOUND',
        price: 89_000,
        discountPercentage: 5,
        rating: null,
        thumbnailUrl: null,
        salesQuantity: 0,
    }],
}

describe('메인 상품 큐레이션', () => {
    beforeEach(() => {
        mockedGetHomeMerchandising.mockReset()
        mockedGetHomeMerchandising.mockResolvedValue(merchandising)
    })

    it('다음 이벤트 버튼으로 프로모션을 전환한다', async () => {
        const user = userEvent.setup()

        render(
            <MemoryRouter>
                <HomeEventCarousel />
            </MemoryRouter>,
        )

        expect(screen.getByRole('heading', { name: /새로운 계절의\s+패션 컬렉션/ })).toBeInTheDocument()

        await user.click(screen.getByRole('button', { name: '다음 이벤트' }))

        expect(screen.getByRole('heading', { name: /매일을 채우는\s+뷰티 루틴/ })).toBeInTheDocument()
        expect(screen.getByRole('link', { name: '뷰티 컬렉션 보기' })).toHaveAttribute('href', '/?categoryId=3')
    })

    it('API 상품을 네 개 큐레이션 섹션에 표시하고 상세 페이지로 연결한다', async () => {
        render(
            <MemoryRouter>
                <HomeMerchandisingSections />
            </MemoryRouter>,
        )

        expect(await screen.findByRole('heading', { name: '카테고리 베스트' })).toBeInTheDocument()
        expect(screen.getByRole('heading', { name: '오늘의 장보기' })).toBeInTheDocument()
        expect(screen.getByRole('heading', { name: '패션 에디트' })).toBeInTheDocument()
        expect(screen.getByRole('heading', { name: '새로 들어온 상품' })).toBeInTheDocument()
        expect(screen.getByRole('link', { name: /카테고리 베스트 재킷/ })).toHaveAttribute('href', '/products/11')
        expect(screen.getByRole('link', { name: /신상품 무선 스피커/ })).toHaveAttribute('href', '/products/41')
    })

    it('카테고리 베스트 상품을 한 슬라이드에 두 개씩 표시한다', async () => {
        render(
            <MemoryRouter>
                <HomeMerchandisingSections />
            </MemoryRouter>,
        )

        expect(await screen.findByRole('heading', { name: '카테고리 베스트 재킷' })).toBeInTheDocument()
        expect(screen.getByRole('heading', { name: '카테고리 베스트 세럼' })).toBeInTheDocument()
        expect(document.querySelector('a[href="/products/11"]')).toHaveAttribute('tabindex', '0')
        expect(document.querySelector('a[href="/products/12"]')).toHaveAttribute('tabindex', '0')
    })

    it('깨진 상품 이미지를 대체 UI로 전환한다', async () => {
        render(
            <MemoryRouter>
                <HomeMerchandisingSections />
            </MemoryRouter>,
        )

        const image = await screen.findByRole('img', { name: '카테고리 베스트 재킷' })
        const productLink = image.closest('a')!
        fireEvent.error(image)

        expect(within(productLink).queryByRole('img')).not.toBeInTheDocument()
        expect(within(productLink).getAllByText('YMALL')).toHaveLength(2)
    })

    it('로딩 중에는 큐레이션 로딩 상태를 표시한다', () => {
        mockedGetHomeMerchandising.mockReturnValue(new Promise(() => undefined))

        render(
            <MemoryRouter>
                <HomeMerchandisingSections />
            </MemoryRouter>,
        )

        expect(screen.getByLabelText('홈 상품 큐레이션 로딩')).toHaveAttribute('aria-busy', 'true')
    })

    it('API 오류를 섹션 안에서 처리하고 다시 요청한다', async () => {
        const user = userEvent.setup()
        mockedGetHomeMerchandising
            .mockRejectedValueOnce(new Error('서버에 연결할 수 없습니다.'))
            .mockResolvedValueOnce(merchandising)

        render(
            <MemoryRouter>
                <HomeMerchandisingSections />
            </MemoryRouter>,
        )

        expect(await screen.findByRole('heading', { name: '추천 상품을 불러오지 못했습니다' })).toBeInTheDocument()
        await user.click(screen.getByRole('button', { name: '다시 시도' }))
        expect(await screen.findByRole('heading', { name: '카테고리 베스트' })).toBeInTheDocument()
        expect(mockedGetHomeMerchandising).toHaveBeenCalledTimes(2)
    })

    it('모든 큐레이션 데이터가 비어 있으면 빈 상태를 표시한다', async () => {
        mockedGetHomeMerchandising.mockResolvedValue({
            categoryBest: [{
                categoryId: 1,
                categoryName: '패션',
                categorySlug: 'fashion',
                products: [],
            }],
            grocery: [],
            fashion: [],
            newArrivals: [],
        })

        render(
            <MemoryRouter>
                <HomeMerchandisingSections />
            </MemoryRouter>,
        )

        expect(await screen.findByRole('heading', { name: '새로운 상품을 준비하고 있습니다' })).toBeInTheDocument()
    })
})
