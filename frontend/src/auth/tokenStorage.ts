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
    try {
        const payloadPart = token.split('.')[1]
        if (!payloadPart) {
            return null
        }

        const normalized = payloadPart.replace(/-/g, '+').replace(/_/g, '/')
        const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
        const payload = JSON.parse(atob(padded)) as { exp?: number }
        return typeof payload.exp === 'number' ? payload.exp * 1000 : null
    } catch {
        return null
    }
}

function isExpired(token: string) {
    const expiration = getTokenExpiration(token)
    return expiration === null || expiration <= Date.now()
}
