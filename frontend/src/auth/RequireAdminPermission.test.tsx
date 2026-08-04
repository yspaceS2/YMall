import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AdminAuthorizationContext } from './AdminAuthorizationContext'
import { RequireAdminPermission } from './RequireAdminPermission'
import type { AdminPermission } from '../types/admin'

function renderGuard(permissions: AdminPermission[], required: AdminPermission) {
    render(
        <AdminAuthorizationContext.Provider value={{
            authorization: {
                memberId: 1,
                adminGrade: 'MANAGER',
                permissions,
            },
            hasPermission: (...targets) =>
                targets.some((permission) => permissions.includes(permission)),
        }}>
            <MemoryRouter initialEntries={['/admin/settlement']}>
                <Routes>
                    <Route path="/forbidden" element={<p>접근 거부</p>} />
                    <Route
                        path="/admin/settlement"
                        element={(
                            <RequireAdminPermission permissions={[required]}>
                                <p>정산 화면</p>
                            </RequireAdminPermission>
                        )}
                    />
                </Routes>
            </MemoryRouter>
        </AdminAuthorizationContext.Provider>,
    )
}

describe('RequireAdminPermission', () => {
    it('필요한 권한이 있으면 화면을 표시한다', () => {
        renderGuard(['SETTLEMENT_REVIEW'], 'SETTLEMENT_REVIEW')

        expect(screen.getByText('정산 화면')).toBeInTheDocument()
    })

    it('필요한 권한이 없으면 접근 거부 화면으로 이동한다', () => {
        renderGuard(['DASHBOARD_READ'], 'SETTLEMENT_REVIEW')

        expect(screen.getByText('접근 거부')).toBeInTheDocument()
        expect(screen.queryByText('정산 화면')).not.toBeInTheDocument()
    })
})
