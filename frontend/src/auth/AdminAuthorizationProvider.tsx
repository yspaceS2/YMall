import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { getAdminAuthorization } from '../api/admin'
import type { AdminAuthorization, AdminPermission } from '../types/admin'
import { AdminAuthorizationContext } from './AdminAuthorizationContext'

export function AdminAuthorizationProvider({ children }: { children: ReactNode }) {
    const [authorization, setAuthorization] = useState<AdminAuthorization | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isDenied, setIsDenied] = useState(false)
    const location = useLocation()

    useEffect(() => {
        const controller = new AbortController()
        getAdminAuthorization(controller.signal)
            .then(setAuthorization)
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setIsDenied(true)
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [])

    const value = useMemo(() => authorization ? {
        authorization,
        hasPermission: (...permissions: AdminPermission[]) =>
            permissions.some((permission) => authorization.permissions.includes(permission)),
    } : null, [authorization])

    if (isLoading) {
        return <div className="min-h-screen bg-paper" aria-label="관리자 권한 확인 중" />
    }
    if (isDenied || !value) {
        return <Navigate to="/forbidden" replace state={{ from: location.pathname }} />
    }
    return (
        <AdminAuthorizationContext.Provider value={value}>
            {children}
        </AdminAuthorizationContext.Provider>
    )
}
