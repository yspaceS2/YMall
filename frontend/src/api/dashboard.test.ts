import { afterEach, describe, expect, it, vi } from 'vitest'
import {
    getAdminDashboardStatistics,
    getSellerDashboardStatistics,
} from './dashboard'

describe('dashboard api', () => {
    afterEach(() => vi.unstubAllGlobals())

    it.each([
        [getSellerDashboardStatistics, '/api/seller/dashboard/statistics', '30d'],
        [getAdminDashboardStatistics, '/api/admin/dashboard/statistics', '6m'],
    ] as const)('기간 조건을 포함해 통계 API를 호출한다', async (request, path, period) => {
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            status: 200,
            json: async () => ({ success: true, data: {}, message: 'ok' }),
        })
        vi.stubGlobal('fetch', fetchMock)

        await request(period)

        const [url] = fetchMock.mock.calls[0] as [string, RequestInit]
        const requestUrl = new URL(url, 'http://localhost')
        expect(requestUrl.pathname).toBe(path)
        expect(requestUrl.searchParams.get('period')).toBe(period)
    })
})
