import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import type { AdminPermission } from '../types/admin'
import { useAdminAuthorization } from './useAdminAuthorization'

export function RequireAdminPermission({
    children,
    permissions,
}: {
    children: ReactNode
    permissions: AdminPermission[]
}) {
    const { hasPermission } = useAdminAuthorization()
    const location = useLocation()

    if (!hasPermission(...permissions)) {
        return <Navigate to="/forbidden" replace state={{ from: location.pathname }} />
    }
    return children
}
