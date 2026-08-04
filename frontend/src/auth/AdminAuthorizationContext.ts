import { createContext } from 'react'
import type { AdminAuthorization, AdminPermission } from '../types/admin'

export interface AdminAuthorizationContextValue {
    authorization: AdminAuthorization
    hasPermission: (...permissions: AdminPermission[]) => boolean
}

export const AdminAuthorizationContext =
    createContext<AdminAuthorizationContextValue | null>(null)
