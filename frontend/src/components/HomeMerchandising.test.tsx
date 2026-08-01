import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import type { ProductSummary } from '../types/product'
import { FashionEditorialCarousel, GroceryEditorialCarousel } from './HomeEditorialCarousels'
import { HomeEventCarousel } from './HomeEventCarousel'
import { TodayRecommendationCarousel } from './TodayRecommendationCarousel'

const products: ProductSummary[] = Array.from({ length: 3 }, (_, index) => ({
    productId: index + 1,
    categoryId: 1,
    categoryName: '패션',
    name: `추천 상품 ${index + 1}`,
    brand: 'YMALL',
    price: 10_000 + index * 1_000,
    discountPercentage: 0,
    rating: null,
    stock: 10,
    thumbnailUrl: null,
    status: 'APPROVED',
}))

describe('메인 상품 큐레이션', () => {
    it('다음 이벤트 버튼으로 프로모션을 전환한다', async () => {
        const user = userEvent.setup()

        render(
            <MemoryRouter>
                <HomeEventCarousel />
            </MemoryRouter>,
        )

        expect(screen.getByRole('heading', { name: /새로운 계절,\s+가벼운 옷차림/ })).toBeInTheDocument()

        await user.click(screen.getByRole('button', { name: '다음 이벤트' }))

        expect(screen.getByRole('heading', { name: /나를 위한\s+매일의 루틴/ })).toBeInTheDocument()
    })

    it('실제 상품 세 개를 추천 슬라이드로 제공한다', async () => {
        const user = userEvent.setup()

        render(
            <MemoryRouter>
                <TodayRecommendationCarousel products={products} />
            </MemoryRouter>,
        )

        expect(screen.getByRole('heading', { name: '추천 상품 1' })).toBeInTheDocument()

        await user.click(screen.getByRole('button', { name: '다음 추천 상품' }))

        expect(screen.getByRole('heading', { name: '추천 상품 2' })).toBeInTheDocument()
        expect(screen.getAllByRole('button', { name: /번 추천 상품 보기/ })).toHaveLength(3)
    })

    it('추천 상품이 하나뿐이면 이동과 자동 재생 조작을 비활성화한다', () => {
        render(
            <MemoryRouter>
                <TodayRecommendationCarousel products={products.slice(0, 1)} />
            </MemoryRouter>,
        )

        expect(screen.getByRole('button', { name: '이전 추천 상품' })).toBeDisabled()
        expect(screen.getByRole('button', { name: '다음 추천 상품' })).toBeDisabled()
        expect(screen.getByRole('button', { name: '추천 상품 자동 재생 일시 정지' })).toBeDisabled()
    })

    it('장보기와 패션 큐레이션을 각각 전환한다', async () => {
        const user = userEvent.setup()

        render(
            <MemoryRouter>
                <GroceryEditorialCarousel />
                <FashionEditorialCarousel />
            </MemoryRouter>,
        )

        expect(screen.getByRole('heading', { name: /오늘의 식탁을\s+더 신선하게/ })).toBeInTheDocument()
        expect(screen.getByRole('heading', { name: /가볍게 시작하는\s+새로운 실루엣/ })).toBeInTheDocument()

        await user.click(screen.getByRole('button', { name: '다음 장보기 큐레이션' }))
        await user.click(screen.getByRole('button', { name: '다음 패션 큐레이션' }))

        expect(screen.getByRole('heading', { name: /바쁜 하루에도\s+든든한 한 끼/ })).toBeInTheDocument()
        expect(screen.getByRole('heading', { name: /주말의 온도를\s+입는 방법/ })).toBeInTheDocument()
    })
})
