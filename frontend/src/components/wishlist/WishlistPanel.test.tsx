import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getWishlist, removeWishlistProduct } from '../../api/wishlist'
import { WishlistPanel } from './WishlistPanel'

vi.mock('../../api/wishlist', () => ({
    getWishlist: vi.fn(),
    removeWishlistProduct: vi.fn(),
}))

const wishlistProducts = [
    {
        productId: 1,
        name: '판매 중지 상품',
        brand: 'YMall',
        price: 39000,
        discountPercentage: 0,
        rating: 4.5,
        stock: 10,
        thumbnailUrl: null,
        status: 'PENDING' as const,
        wishedAt: '2026-07-27T12:00:00',
    },
    {
        productId: 2,
        name: '품절 상품',
        brand: 'YMall',
        price: 29000,
        discountPercentage: 10,
        rating: null,
        stock: 0,
        thumbnailUrl: null,
        status: 'APPROVED' as const,
        wishedAt: '2026-07-26T12:00:00',
    },
]

describe('WishlistPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('찜 상품의 판매 상태를 표시하고 선택한 상품을 해제한다', async () => {
        vi.mocked(getWishlist)
            .mockResolvedValueOnce(wishlistPage(wishlistProducts, 1))
            .mockResolvedValueOnce(wishlistPage([wishlistProducts[1]], 1))
        vi.mocked(removeWishlistProduct).mockResolvedValue(undefined)

        render(
            <MemoryRouter>
                <WishlistPanel />
            </MemoryRouter>,
        )

        expect(await screen.findByText('판매 중지 상품')).toBeInTheDocument()
        expect(screen.getByText('판매 중지')).toBeInTheDocument()
        expect(screen.getByText('품절')).toBeInTheDocument()

        fireEvent.click(screen.getByRole('button', { name: '판매 중지 상품 찜 해제' }))

        await waitFor(() => {
            expect(removeWishlistProduct).toHaveBeenCalledWith(1)
            expect(getWishlist).toHaveBeenCalledTimes(2)
            expect(screen.queryByText('판매 중지 상품')).not.toBeInTheDocument()
        })
    })

    it('여러 페이지를 불러온 뒤 상품을 해제하면 첫 페이지부터 다시 조회한다', async () => {
        vi.mocked(getWishlist)
            .mockResolvedValueOnce(wishlistPage([wishlistProducts[0]], 1, true))
            .mockResolvedValueOnce(wishlistPage([wishlistProducts[1]], 2))
            .mockResolvedValueOnce(wishlistPage([wishlistProducts[1]], 1))
        vi.mocked(removeWishlistProduct).mockResolvedValue(undefined)

        render(
            <MemoryRouter>
                <WishlistPanel />
            </MemoryRouter>,
        )

        expect(await screen.findByText('판매 중지 상품')).toBeInTheDocument()
        fireEvent.click(screen.getByRole('button', { name: '찜 상품 더 보기' }))
        expect(await screen.findByText('품절 상품')).toBeInTheDocument()

        fireEvent.click(screen.getByRole('button', { name: '판매 중지 상품 찜 해제' }))

        await waitFor(() => {
            expect(getWishlist).toHaveBeenCalledTimes(3)
            expect(vi.mocked(getWishlist).mock.calls[2]?.[0]).toBe(1)
            expect(screen.queryByText('판매 중지 상품')).not.toBeInTheDocument()
            expect(screen.getByText('품절 상품')).toBeInTheDocument()
        })
    })
})

function wishlistPage(
    content: typeof wishlistProducts,
    page: number,
    hasNext = false,
) {
    return {
        content,
        page,
        size: 8,
        totalElements: content.length,
        totalPages: hasNext ? page + 1 : page,
        hasNext,
        hasPrevious: page > 1,
    }
}
