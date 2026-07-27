import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
    addWishlistProduct,
    getWishlistStatus,
    removeWishlistProduct,
} from '../../api/wishlist'
import { ProductWishlistButton } from './ProductWishlistButton'

vi.mock('../../api/wishlist', () => ({
    getWishlistStatus: vi.fn(),
    addWishlistProduct: vi.fn(),
    removeWishlistProduct: vi.fn(),
}))

describe('ProductWishlistButton', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('로그인 회원의 저장된 찜 상태를 조회하고 등록·해제한다', async () => {
        vi.mocked(getWishlistStatus).mockResolvedValue({
            productId: 1,
            wished: false,
        })
        vi.mocked(addWishlistProduct).mockResolvedValue({
            productId: 1,
            wished: true,
        })
        vi.mocked(removeWishlistProduct).mockResolvedValue(undefined)

        render(
            <ProductWishlistButton
                productId={1}
                isAuthenticated
                onLoginRequired={vi.fn()}
                onError={vi.fn()}
            />,
        )

        const addButton = await screen.findByRole('button', { name: '찜하기' })
        await waitFor(() => expect(addButton).toBeEnabled())
        fireEvent.click(addButton)

        expect(await screen.findByRole('button', { name: '찜 해제' })).toHaveAttribute(
            'aria-pressed',
            'true',
        )
        expect(addWishlistProduct).toHaveBeenCalledWith(1)

        fireEvent.click(screen.getByRole('button', { name: '찜 해제' }))

        await waitFor(() => {
            expect(removeWishlistProduct).toHaveBeenCalledWith(1)
            expect(screen.getByRole('button', { name: '찜하기' })).toHaveAttribute(
                'aria-pressed',
                'false',
            )
        })
    })

    it('비로그인 상태에서는 API 대신 로그인 이동을 요청한다', () => {
        const onLoginRequired = vi.fn()

        render(
            <ProductWishlistButton
                productId={1}
                isAuthenticated={false}
                onLoginRequired={onLoginRequired}
                onError={vi.fn()}
            />,
        )
        fireEvent.click(screen.getByRole('button', { name: '찜하기' }))

        expect(onLoginRequired).toHaveBeenCalledOnce()
        expect(getWishlistStatus).not.toHaveBeenCalled()
        expect(addWishlistProduct).not.toHaveBeenCalled()
    })
})
