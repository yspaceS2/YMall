import { clearAccessTokenIfMatches, getAccessToken, notifyUnauthorized } from '../auth/tokenStorage'
import type { ApiResponse, ErrorResponse } from '../types/api'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '')

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

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...requestInit,
        credentials: requestInit.credentials ?? 'include',
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
    })

    if (!response.ok) {
        const error = (await response.json().catch(() => null)) as ErrorResponse | null
        if (response.status === 401 && auth) {
            if (token) {
                clearAccessTokenIfMatches(token)
                if (getAccessToken() === null) {
                    notifyUnauthorized()
                }
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
