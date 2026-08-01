import { afterEach, describe, expect, it, vi } from 'vitest'

import { getAccessToken, setAccessToken } from '../auth/tokenStorage'
import { apiRequest } from './client'

function token(subject: string) {
    const header = btoa(JSON.stringify({ alg: 'none', typ: 'JWT' }))
    const payload = btoa(JSON.stringify({
        sub: subject,
        role: 'ROLE_SELLER',
        exp: Math.floor(Date.now() / 1000) + 3_600,
    }))
    return `${header}.${payload}.signature`
}

function successResponse() {
    return new Response(JSON.stringify({
        success: true,
        data: { content: [] },
        message: 'ok',
    }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
    })
}

describe('apiRequest authentication recovery', () => {
    afterEach(() => {
        localStorage.clear()
        vi.unstubAllGlobals()
    })

    it('다른 요청이 갱신한 토큰을 오래된 401 응답이 삭제하지 않는다', async () => {
        const oldToken = token('old')
        const refreshedToken = token('refreshed')
        setAccessToken(oldToken)

        const fetchMock = vi.fn().mockImplementation(async (_url: string, init?: RequestInit) => {
            const authorization = new Headers(init?.headers).get('Authorization')
            if (authorization === `Bearer ${oldToken}`) {
                setAccessToken(refreshedToken)
                return new Response(null, { status: 401 })
            }
            if (authorization === `Bearer ${refreshedToken}`) {
                return successResponse()
            }
            throw new Error(`Unexpected authorization: ${authorization}`)
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(apiRequest('/seller/settlement-requests'))
            .resolves.toEqual({ content: [] })
        expect(getAccessToken()).toBe(refreshedToken)
        expect(fetchMock).toHaveBeenCalledTimes(2)
        expect(fetchMock.mock.calls.some(([url]) =>
            String(url).endsWith('/members/tokens/refresh'))).toBe(false)
    })
})
