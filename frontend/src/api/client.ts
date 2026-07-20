import { clearAccessTokenIfMatches, getAccessToken, notifyUnauthorized, setAccessToken } from '../auth/tokenStorage'
import type { ApiResponse, ErrorResponse } from '../types/api'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '')
let refreshPromise: Promise<string | null> | null = null

interface ApiRequestOptions extends Omit<RequestInit, 'body'> {
    body?: unknown
    auth?: boolean
}

export class ApiError extends Error {
    readonly status: number
    readonly code?: string

    constructor(message: string, status: number, code?: string) {
        super(message)
        this.name = 'ApiError'
        this.status = status
        this.code = code
    }
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
    const { body, auth = true, headers: initialHeaders, ...requestInit } = options
    const headers = new Headers(initialHeaders)
    const token = auth ? getAccessToken() : null

    if (body !== undefined) {
        headers.set('Content-Type', 'application/json')
    }
    if (token) {
        headers.set('Authorization', `Bearer ${token}`)
    }

    const request = (accessToken: string | null) => {
        const requestHeaders = new Headers(headers)
        if (accessToken) {
            requestHeaders.set('Authorization', `Bearer ${accessToken}`)
        } else {
            requestHeaders.delete('Authorization')
        }
        return fetch(`${API_BASE_URL}${path}`, {
            ...requestInit,
            credentials: requestInit.credentials ?? 'include',
            headers: requestHeaders,
            body: body === undefined ? undefined : JSON.stringify(body),
        })
    }

    let response = await request(token)
    if (response.status === 401 && auth && path !== '/members/tokens/refresh') {
        const refreshedToken = await refreshAccessToken()
        if (refreshedToken) {
            setAccessToken(refreshedToken)
            response = await request(refreshedToken)
        }
    }

    if (!response.ok) {
        const error = (await response.json().catch(() => null)) as ErrorResponse | null
        if (response.status === 401 && auth) {
            const currentToken = getAccessToken()
            if (currentToken) {
                clearAccessTokenIfMatches(currentToken)
            }
            if (getAccessToken() === null) {
                notifyUnauthorized()
            }
        }
        throw new ApiError(
            error?.error.message ?? '요청을 처리하지 못했습니다.',
            response.status,
            error?.error.code,
        )
    }

    if (response.status === 204) {
        return undefined as T
    }

    const result = (await response.json()) as ApiResponse<T>
    return result.data
}

export async function refreshAccessToken() {
    if (refreshPromise === null) {
        refreshPromise = fetch(`${API_BASE_URL}/members/tokens/refresh`, {
            method: 'POST',
            credentials: 'include',
        }).then(async (response) => {
            if (!response.ok) {
                return null
            }
            const result = (await response.json()) as ApiResponse<{ accessToken: string }>
            return result.data.accessToken
        }).finally(() => {
            refreshPromise = null
        })
    }
    return refreshPromise
}
