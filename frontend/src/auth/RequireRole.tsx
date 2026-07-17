import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import type { MemberRole } from '../types/auth'
import { useAuth } from './useAuth'

export function RequireRole({ children, roles }: { children: ReactNode; roles: MemberRole[] }) {
    const { isAuthenticated, role } = useAuth()
    const location = useLocation()

    if (!isAuthenticated) {
        return <Navigate to="/login" replace state={{ from: location.pathname }} />
    }
    if (!role || !roles.includes(role)) {
        return <Navigate to="/" replace />
    }
    return children
}
