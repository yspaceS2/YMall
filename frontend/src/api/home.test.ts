import { afterEach, describe, expect, it, vi } from 'vitest'
import { getHomeMerchandising } from './home'

describe('home merchandising api', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
    })

    it('인증 토큰 없이 홈 큐레이션 API를 요청한다', async () => {
        const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
            success: true,
            data: {
                categoryBest: [],
                grocery: [],
                fashion: [],
                newArrivals: [],
            },
            message: null,
        }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
        }))
        vi.stubGlobal('fetch', fetchMock)

        await getHomeMerchandising()

        expect(fetchMock).toHaveBeenCalledWith('/api/home/merchandising', expect.objectContaining({
            credentials: 'include',
        }))
        const request = fetchMock.mock.calls[0]?.[1] as RequestInit
        expect(new Headers(request.headers).has('Authorization')).toBe(false)
    })
})
