import { ApiError } from './client'

export function isAbortError(error: unknown): error is Error {
    return error instanceof Error && error.name === 'AbortError'
}

export function getApiErrorMessage(error: unknown, fallback: string) {
    return error instanceof ApiError ? error.message : fallback
}
