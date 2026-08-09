import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import type { MemberRole } from '../types/auth'
import { useAuth } from './useAuth'

/**
 * 인증 상태와 역할에 따라 화면 진입을 안내하는 Presentation Guard이다.
 * Backend 권한 검증을 대체하지 않으며, 보호 API는 서버에서 역할과 소유권을 다시 확인해야 한다.
 */
export function RequireRole({ children, roles }: { children: ReactNode; roles: MemberRole[] }) {
    const { isAuthenticated, isLoggingOut, role } = useAuth()
    const location = useLocation()

    if (!isAuthenticated) {
        if (isLoggingOut) {
            return <Navigate to="/" replace />
        }
        return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />
    }
    if (!role || !roles.includes(role)) {
        return <Navigate to="/forbidden" replace state={{ from: location.pathname }} />
    }
    return children
}
