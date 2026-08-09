import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import type { AdminPermission } from '../types/admin'
import { useAdminAuthorization } from './useAdminAuthorization'

/**
 * 관리자 화면의 필요 권한을 확인하는 Client Route Guard이다.
 * 메뉴와 Route 노출을 제어할 뿐이며 실제 관리자 작업의 허가는 Backend 응답을 기준으로 한다.
 */
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
