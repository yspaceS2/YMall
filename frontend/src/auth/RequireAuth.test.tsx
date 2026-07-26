import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from './AuthContext'
import { RequireAuth } from './RequireAuth'

function renderRoute(isAuthenticated: boolean) {
    const auth: AuthContextValue = {
        isAuthenticated,
        role: isAuthenticated ? 'ROLE_USER' : null,
        login: vi.fn(),
        completeOAuthLogin: vi.fn(),
        logout: vi.fn(),
    }

    return render(
        <AuthContext.Provider value={auth}>
            <MemoryRouter initialEntries={['/cart']}>
                <Routes>
                    <Route path="/login" element={<p>로그인 화면</p>} />
                    <Route
                        path="/cart"
                        element={<RequireAuth><p>장바구니 화면</p></RequireAuth>}
                    />
                </Routes>
            </MemoryRouter>
        </AuthContext.Provider>,
    )
}

describe('RequireAuth', () => {
    it('로그인하지 않은 사용자를 로그인 화면으로 보낸다', () => {
        renderRoute(false)

        expect(screen.getByText('로그인 화면')).toBeInTheDocument()
        expect(screen.queryByText('장바구니 화면')).not.toBeInTheDocument()
    })

    it('로그인한 사용자에게 보호 화면을 보여준다', () => {
        renderRoute(true)

        expect(screen.getByText('장바구니 화면')).toBeInTheDocument()
    })
})
