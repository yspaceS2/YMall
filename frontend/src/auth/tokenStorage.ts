import type { MemberRole } from '../types/auth'

const ACCESS_TOKEN_KEY = 'ymall.accessToken'

export const AUTH_CHANGED_EVENT = 'ymall:auth-changed'
export const AUTH_UNAUTHORIZED_EVENT = 'ymall:auth-unauthorized'

export function getAccessToken() {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY)
    if (token && isExpired(token)) {
        clearAccessToken()
        return null
    }
    return token
}

export function setAccessToken(token: string) {
    localStorage.setItem(ACCESS_TOKEN_KEY, token)
    window.dispatchEvent(new Event(AUTH_CHANGED_EVENT))
}

export function clearAccessToken() {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    window.dispatchEvent(new Event(AUTH_CHANGED_EVENT))
}

export function notifyUnauthorized() {
    window.dispatchEvent(new Event(AUTH_UNAUTHORIZED_EVENT))
}

export function getTokenExpiration(token: string) {
    const payload = getTokenPayload(token)
    return typeof payload?.exp === 'number' ? payload.exp * 1000 : null
}

export function getTokenRole(token: string | null): MemberRole | null {
    if (!token) return null
    const role = getTokenPayload(token)?.role
    return role === 'ROLE_USER' || role === 'ROLE_SELLER' || role === 'ROLE_ADMIN'
        ? role
        : null
}

function getTokenPayload(token: string): { exp?: number; role?: unknown } | null {
    try {
        const payloadPart = token.split('.')[1]
        if (!payloadPart) {
            return null
        }

        const normalized = payloadPart.replace(/-/g, '+').replace(/_/g, '/')
        const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
        return JSON.parse(atob(padded)) as { exp?: number; role?: unknown }
    } catch {
        return null
    }
}

function isExpired(token: string) {
    const expiration = getTokenExpiration(token)
    return expiration === null || expiration <= Date.now()
}
