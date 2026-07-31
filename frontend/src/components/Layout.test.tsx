import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../auth/AuthContext'
import { ThemeProvider } from '../theme/ThemeProvider'
import type { MemberRole } from '../types/auth'
import { Layout } from './Layout'

const getUnreadNotificationCount = vi.fn()
const getCategories = vi.fn()
const getCart = vi.fn()

vi.mock('../api/cart', () => ({
    CART_CHANGED_EVENT: 'ymall:cart-changed',
    getCart: (...args: unknown[]) => getCart(...args),
}))

vi.mock('../api/notifications', () => ({
    NOTIFICATIONS_CHANGED_EVENT: 'ymall:notifications-changed',
    getUnreadNotificationCount: (...args: unknown[]) => getUnreadNotificationCount(...args),
}))

vi.mock('../api/products', () => ({
    getCategories: (...args: unknown[]) => getCategories(...args),
}))

function renderLayout(role: MemberRole | null) {
    const logout = vi.fn()
    const auth: AuthContextValue = {
        isAuthenticated: role !== null,
        role,
        login: vi.fn(),
        completeOAuthLogin: vi.fn(),
        logout,
    }

    const result = render(
        <ThemeProvider>
            <AuthContext.Provider value={auth}>
                <MemoryRouter>
                    <Layout><p>본문</p></Layout>
                </MemoryRouter>
            </AuthContext.Provider>
        </ThemeProvider>,
    )
    return { ...result, logout }
}

describe('Layout 역할별 메뉴', () => {
    it('일반 사용자에게 판매자·관리자 메뉴를 숨기고 미읽음 배지를 표시한다', async () => {
        getUnreadNotificationCount.mockResolvedValue({ unreadCount: 3 })
        getCart.mockResolvedValue({ items: [] })
        getCategories.mockResolvedValue([])

        renderLayout('ROLE_USER')

        expect(screen.queryByRole('menuitem', { name: '판매자 센터' })).not.toBeInTheDocument()
        expect(screen.queryByRole('menuitem', { name: '관리자 콘솔' })).not.toBeInTheDocument()
        await waitFor(() => {
            expect(screen.getByText('3')).toBeInTheDocument()
        })
    })

    it('판매자에게 판매자 메뉴만 표시한다', () => {
        getUnreadNotificationCount.mockResolvedValue({ unreadCount: 0 })
        getCart.mockResolvedValue({ items: [] })
        getCategories.mockResolvedValue([])

        renderLayout('ROLE_SELLER')

        expect(screen.getByRole('menuitem', { name: '판매자 센터' })).toBeInTheDocument()
        expect(screen.queryByRole('menuitem', { name: '관리자 콘솔' })).not.toBeInTheDocument()
    })

    it('관리자에게 판매자·관리자 메뉴를 모두 표시한다', () => {
        getUnreadNotificationCount.mockResolvedValue({ unreadCount: 0 })
        getCart.mockResolvedValue({ items: [] })
        getCategories.mockResolvedValue([])

        renderLayout('ROLE_ADMIN')

        expect(screen.getByRole('menuitem', { name: '판매자 센터' })).toBeInTheDocument()
        expect(screen.getByRole('menuitem', { name: '관리자 콘솔' })).toBeInTheDocument()
    })

    it('로그아웃 버튼을 누르면 인증 로그아웃을 요청한다', async () => {
        getUnreadNotificationCount.mockResolvedValue({ unreadCount: 0 })
        getCart.mockResolvedValue({ items: [] })
        getCategories.mockResolvedValue([])
        const user = userEvent.setup()
        const { logout } = renderLayout('ROLE_USER')

        await user.click(screen.getByRole('menuitem', { name: '로그아웃' }))

        expect(logout).toHaveBeenCalledOnce()
    })

    it('스토어 핵심 메뉴와 카테고리를 제공한다', async () => {
        getUnreadNotificationCount.mockResolvedValue({ unreadCount: 0 })
        getCart.mockResolvedValue({
            items: [
                { cartItemId: 1, quantity: 2 },
                { cartItemId: 2, quantity: 1 },
            ],
        })
        getCategories.mockResolvedValue([
            {
                categoryId: 1,
                name: '패션',
                slug: 'fashion',
                parentId: null,
                depth: 1,
                displayOrder: 1,
            },
            {
                categoryId: 2,
                name: '여성패션',
                slug: 'women-fashion',
                parentId: 1,
                depth: 2,
                displayOrder: 1,
            },
            {
                categoryId: 3,
                name: '아우터',
                slug: 'women-outer',
                parentId: 2,
                depth: 3,
                displayOrder: 1,
            },
        ])
        const user = userEvent.setup()

        renderLayout('ROLE_USER')

        expect(screen.getByRole('link', { name: 'YMall 홈' })).toBeInTheDocument()
        expect(screen.getByRole('search', { name: '통합 상품 검색' })).toBeInTheDocument()
        expect(screen.getByRole('link', { name: '찜한 상품' })).toHaveAttribute('href', '/mypage#wishlist')
        expect(screen.getByRole('link', { name: '장바구니' })).toHaveAttribute('href', '/cart')
        expect(await screen.findByLabelText('장바구니 상품 3개')).toHaveTextContent('3')
        screen.getAllByRole('link', { name: '내 정보' }).forEach((link) => {
            expect(link).toHaveAttribute('href', '/mypage')
        })

        await user.click(screen.getByRole('button', { name: '전체 카테고리 열기' }))

        expect(await screen.findByRole('button', { name: '패션' })).toBeInTheDocument()
        expect(screen.queryByRole('navigation', { name: '패션 중분류' })).not.toBeInTheDocument()

        await user.hover(screen.getByRole('button', { name: '패션' }))

        expect(screen.getByRole('link', { name: '패션 전체' })).toHaveAttribute('href', '/?categoryId=1')
        expect(screen.queryByRole('navigation', { name: '여성패션 소분류' })).not.toBeInTheDocument()

        await user.hover(screen.getByRole('button', { name: '여성패션' }))

        expect(screen.getByRole('link', { name: '아우터' })).toBeInTheDocument()
        expect(screen.getAllByRole('link', { name: '내 정보' })).not.toHaveLength(0)
        expect(screen.getByRole('button', { name: '전체 카테고리 닫기' })).toBeInTheDocument()
    })
})
