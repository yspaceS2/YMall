import { useContext } from 'react'
import { AdminAuthorizationContext } from './AdminAuthorizationContext'

export function useAdminAuthorization() {
    const context = useContext(AdminAuthorizationContext)
    if (!context) {
        throw new Error('useAdminAuthorization must be used within AdminAuthorizationProvider')
    }
    return context
}

export function useOptionalAdminAuthorization() {
    return useContext(AdminAuthorizationContext)
}
