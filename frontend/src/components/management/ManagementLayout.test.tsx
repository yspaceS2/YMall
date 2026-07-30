import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../../auth/AuthContext'
import { ThemeProvider } from '../../theme/ThemeProvider'
import { ManagementLayout } from './ManagementLayout'

function renderManagementLayout(
    role: 'member' | 'seller' | 'admin',
    initialPath?: string,
) {
    const logout = vi.fn()
    const auth: AuthContextValue = {
        isAuthenticated: true,
        role: role === 'admin'
            ? 'ROLE_ADMIN'
            : role === 'seller'
                ? 'ROLE_SELLER'
                : 'ROLE_USER',
        login: vi.fn(),
        completeOAuthLogin: vi.fn(),
        logout,
    }

    render(
        <ThemeProvider>
            <AuthContext.Provider value={auth}>
                <MemoryRouter initialEntries={[
                    initialPath
                        ?? (role === 'member'
                            ? '/mypage'
                            : role === 'seller'
                                ? '/seller'
                                : '/admin'),
                ]}>
                    <ManagementLayout role={role}>
                        <p>관리 콘텐츠</p>
                    </ManagementLayout>
                </MemoryRouter>
            </AuthContext.Provider>
        </ThemeProvider>,
    )

    return { logout }
}

describe('ManagementLayout', () => {
    it('회원 마이페이지에 현재 제공하는 계정 메뉴를 표시한다', () => {
        renderManagementLayout('member')

        expect(screen.getByRole('complementary', { name: '마이페이지 메뉴' }))
            .toBeInTheDocument()
        expect(screen.getByRole('link', { name: '대시보드' }))
            .toHaveAttribute('href', '/mypage')
        expect(screen.getByRole('link', { name: '주문·배송 조회' }))
            .toHaveAttribute('href', '/mypage/orders')
        expect(screen.getByRole('link', { name: '알림' }))
            .toHaveAttribute('href', '/mypage/notifications')
        expect(screen.getByRole('link', { name: '판매자 신청' }))
            .toHaveAttribute('href', '/mypage/seller-application')
    })

    it.each([
        ['seller', '판매자 센터 메뉴', '/seller'],
        ['admin', '관리자 센터 메뉴', '/admin'],
    ] as const)('%s 센터에 독립된 대시보드 메뉴를 표시한다', (role, label, href) => {
        renderManagementLayout(role)

        expect(screen.getByRole('complementary', { name: label })).toBeInTheDocument()
        expect(screen.getByRole('link', { name: '대시보드' }))
            .toHaveAttribute('href', href)
        expect(screen.getByText('관리 콘텐츠')).toBeInTheDocument()
    })

    it('현재 경로에 해당하는 메뉴만 활성화한다', () => {
        renderManagementLayout('member', '/mypage/notifications')

        expect(screen.getByRole('link', { name: '알림' }))
            .toHaveAttribute('aria-current', 'page')
        expect(screen.getByRole('link', { name: '대시보드' }))
            .not.toHaveAttribute('aria-current')
    })

    it('관리자에게 판매자 신청 관리 메뉴를 표시한다', () => {
        renderManagementLayout('admin', '/admin/seller-applications')

        expect(screen.getByRole('link', { name: '판매자 신청 관리' }))
            .toHaveAttribute('href', '/admin/seller-applications')
        expect(screen.getByRole('link', { name: '판매자 신청 관리' }))
            .toHaveAttribute('aria-current', 'page')
    })

    it('로그아웃 버튼으로 인증 로그아웃을 요청한다', async () => {
        const user = userEvent.setup()
        const { logout } = renderManagementLayout('admin')

        await user.click(screen.getByRole('button', { name: '로그아웃' }))

        expect(logout).toHaveBeenCalledOnce()
    })
})
