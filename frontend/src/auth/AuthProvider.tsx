import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { loginMember } from '../api/auth'
import type { LoginRequest } from '../types/auth'
import { AuthContext } from './AuthContext'
import {
    AUTH_CHANGED_EVENT,
    AUTH_UNAUTHORIZED_EVENT,
    clearAccessToken,
    getAccessToken,
    getTokenExpiration,
    setAccessToken,
} from './tokenStorage'

export function AuthProvider({ children }: { children: ReactNode }) {
    const [isAuthenticated, setIsAuthenticated] = useState(() => getAccessToken() !== null)
    const location = useLocation()
    const navigate = useNavigate()

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

            expirationTimer = window.setTimeout(
                clearAccessToken,
                Math.max(expiration - Date.now(), 0),
            )
        }
        const syncAuthentication = () => {
            const token = getAccessToken()
            setIsAuthenticated(token !== null)
            scheduleExpiration(token)
        }
        const handleUnauthorized = () => {
            setIsAuthenticated(false)
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
    }, [])

    const logout = useCallback(() => {
        clearAccessToken()
        setIsAuthenticated(false)
        navigate('/', { replace: true })
    }, [navigate])

    const value = useMemo(
        () => ({ isAuthenticated, login, logout }),
        [isAuthenticated, login, logout],
    )

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
