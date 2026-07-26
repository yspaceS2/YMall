import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../auth/AuthContext'
import type { MemberRole } from '../types/auth'
import { Layout } from './Layout'

const getUnreadNotificationCount = vi.fn()

vi.mock('../api/notifications', () => ({
    NOTIFICATIONS_CHANGED_EVENT: 'ymall:notifications-changed',
    getUnreadNotificationCount: (...args: unknown[]) => getUnreadNotificationCount(...args),
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
        <AuthContext.Provider value={auth}>
            <MemoryRouter>
                <Layout><p>본문</p></Layout>
            </MemoryRouter>
        </AuthContext.Provider>,
    )
    return { ...result, logout }
}

describe('Layout 역할별 메뉴', () => {
    it('일반 사용자에게 판매자·관리자 메뉴를 숨기고 미읽음 배지를 표시한다', async () => {
        getUnreadNotificationCount.mockResolvedValue({ unreadCount: 3 })

        renderLayout('ROLE_USER')

        expect(screen.queryByRole('link', { name: '판매자 관리' })).not.toBeInTheDocument()
        expect(screen.queryByRole('link', { name: '관리자 운영' })).not.toBeInTheDocument()
        await waitFor(() => {
            expect(screen.getByRole('link', { name: '알림 3개' })).toBeInTheDocument()
        })
    })

    it('판매자에게 판매자 메뉴만 표시한다', () => {
        getUnreadNotificationCount.mockResolvedValue({ unreadCount: 0 })

        renderLayout('ROLE_SELLER')

        expect(screen.getByRole('link', { name: '판매자 관리' })).toBeInTheDocument()
        expect(screen.queryByRole('link', { name: '관리자 운영' })).not.toBeInTheDocument()
    })

    it('관리자에게 판매자·관리자 메뉴를 모두 표시한다', () => {
        getUnreadNotificationCount.mockResolvedValue({ unreadCount: 0 })

        renderLayout('ROLE_ADMIN')

        expect(screen.getByRole('link', { name: '판매자 관리' })).toBeInTheDocument()
        expect(screen.getByRole('link', { name: '관리자 운영' })).toBeInTheDocument()
    })

    it('로그아웃 버튼을 누르면 인증 로그아웃을 요청한다', async () => {
        getUnreadNotificationCount.mockResolvedValue({ unreadCount: 0 })
        const user = userEvent.setup()
        const { logout } = renderLayout('ROLE_USER')

        await user.click(screen.getByRole('button', { name: '로그아웃' }))

        expect(logout).toHaveBeenCalledOnce()
    })
})
