import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { loginMember, logoutMember } from '../api/auth'
import { refreshAccessToken } from '../api/client'
import type { LoginRequest } from '../types/auth'
import { AuthContext } from './AuthContext'
import {
    AUTH_CHANGED_EVENT,
    AUTH_LOGOUT_COMPLETED_EVENT,
    AUTH_UNAUTHORIZED_EVENT,
    clearAccessToken,
    getAccessToken,
    getTokenExpiration,
    getTokenRole,
    setAccessToken,
} from './tokenStorage'

export function AuthProvider({ children }: { children: ReactNode }) {
    const initialToken = getAccessToken()
    const [isAuthenticated, setIsAuthenticated] = useState(() => initialToken !== null)
    const [role, setRole] = useState(() => getTokenRole(initialToken))
    const location = useLocation()
    const navigate = useNavigate()

    useEffect(() => {
        if (getAccessToken() !== null) {
            return
        }
        refreshAccessToken()
            .then((accessToken) => {
                if (!accessToken) {
                    return
                }
                setAccessToken(accessToken)
                setIsAuthenticated(true)
                setRole(getTokenRole(accessToken))
            })
            .catch(() => undefined)
    }, [])

    useEffect(() => {
        let expirationTimer: number | undefined

        const scheduleExpiration = (token: string | null) => {
            if (expirationTimer !== undefined) {
                window.clearTimeout(expirationTimer)
                expirationTimer = undefined
            }
            if (!token) {
                return
            }

            const expiration = getTokenExpiration(token)
            if (expiration === null) {
                clearAccessToken()
                return
            }

            expirationTimer = window.setTimeout(async () => {
                try {
                    const refreshedToken = await refreshAccessToken()
                    if (refreshedToken) {
                        setAccessToken(refreshedToken)
                    } else {
                        clearAccessToken()
                    }
                } catch {
                    clearAccessToken()
                }
            }, Math.max(expiration - Date.now() - 5_000, 0))
        }
        const syncAuthentication = () => {
            const token = getAccessToken()
            setIsAuthenticated(token !== null)
            setRole(getTokenRole(token))
            scheduleExpiration(token)
        }
        const handleUnauthorized = () => {
            setIsAuthenticated(false)
            setRole(null)
            if (location.pathname !== '/login') {
                navigate('/login', {
                    replace: true,
                    state: { from: `${location.pathname}${location.search}` },
                })
            }
        }

        window.addEventListener(AUTH_CHANGED_EVENT, syncAuthentication)
        window.addEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized)
        window.addEventListener('storage', syncAuthentication)
        scheduleExpiration(getAccessToken())
        return () => {
            if (expirationTimer !== undefined) {
                window.clearTimeout(expirationTimer)
            }
            window.removeEventListener(AUTH_CHANGED_EVENT, syncAuthentication)
            window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized)
            window.removeEventListener('storage', syncAuthentication)
        }
    }, [location.pathname, location.search, navigate])

    const login = useCallback(async (request: LoginRequest) => {
        const response = await loginMember(request)
        setAccessToken(response.accessToken)
        setIsAuthenticated(true)
        setRole(getTokenRole(response.accessToken))
    }, [])

    const completeOAuthLogin = useCallback((accessToken: string) => {
        setAccessToken(accessToken)
        setIsAuthenticated(true)
        setRole(getTokenRole(accessToken))
    }, [])

    const logout = useCallback(async () => {
        try {
            await logoutMember()
        } finally {
            clearAccessToken()
            setIsAuthenticated(false)
            setRole(null)
            navigate('/', { replace: true })
            window.dispatchEvent(new Event(AUTH_LOGOUT_COMPLETED_EVENT))
        }
    }, [navigate])

    const value = useMemo(
        () => ({ isAuthenticated, role, login, completeOAuthLogin, logout }),
        [isAuthenticated, role, login, completeOAuthLogin, logout],
    )

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
