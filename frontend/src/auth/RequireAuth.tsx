import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth'

export function RequireAuth({ children }: { children: ReactNode }) {
    const { isAuthenticated, isLoggingOut } = useAuth()
    const location = useLocation()

    if (!isAuthenticated) {
        if (isLoggingOut) {
            return <Navigate to="/" replace />
        }
        return (
            <Navigate
                to="/login"
                replace
                state={{ from: `${location.pathname}${location.search}` }}
            />
        )
    }

    return children
}
