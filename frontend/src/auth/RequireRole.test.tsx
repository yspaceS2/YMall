import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { MemberRole } from '../types/auth'
import { AuthContext, type AuthContextValue } from './AuthContext'
import { RequireRole } from './RequireRole'

function renderRoute(role: MemberRole | null) {
    const auth: AuthContextValue = {
        isAuthenticated: role !== null,
        role,
        login: vi.fn(),
        completeOAuthLogin: vi.fn(),
        logout: vi.fn(),
    }

    return render(
        <AuthContext.Provider value={auth}>
            <MemoryRouter initialEntries={['/seller']}>
                <Routes>
                    <Route path="/login" element={<p>로그인 화면</p>} />
                    <Route path="/forbidden" element={<p>접근 제한 화면</p>} />
                    <Route
                        path="/seller"
                        element={(
                            <RequireRole roles={['ROLE_SELLER', 'ROLE_ADMIN']}>
                                <p>판매자 화면</p>
                            </RequireRole>
                        )}
                    />
                </Routes>
            </MemoryRouter>
        </AuthContext.Provider>,
    )
}

describe('RequireRole', () => {
    it('일반 사용자의 판매자 화면 접근을 차단한다', () => {
        renderRoute('ROLE_USER')

        expect(screen.getByText('접근 제한 화면')).toBeInTheDocument()
    })

    it.each<MemberRole>(['ROLE_SELLER', 'ROLE_ADMIN'])(
        '%s 역할에는 판매자 화면을 허용한다',
        (role) => {
            renderRoute(role)

            expect(screen.getByText('판매자 화면')).toBeInTheDocument()
        },
    )
})
