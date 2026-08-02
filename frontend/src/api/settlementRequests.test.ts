import { afterEach, describe, expect, it, vi } from 'vitest'

import { getAdminSettlementRequests } from './admin'
import { getSettlementRequests } from './seller'

function pageResponse() {
    return new Response(JSON.stringify({
        success: true,
        data: {
            content: [],
            page: 1,
            size: 20,
            totalElements: 0,
            totalPages: 0,
        },
        message: 'ok',
    }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
    })
}

describe('settlement request filters', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
    })

    it('includes the requested date range for a seller', async () => {
        const fetchMock = vi.fn().mockResolvedValue(pageResponse())
        vi.stubGlobal('fetch', fetchMock)

        await getSettlementRequests({
            requestedFrom: '2026-07-01',
            requestedTo: '2026-07-30',
        })

        const [url] = fetchMock.mock.calls[0] as [string, RequestInit]
        const requestUrl = new URL(url, 'http://localhost')
        expect(requestUrl.pathname).toBe('/api/seller/settlement-requests')
        expect(requestUrl.searchParams.get('requestedFrom')).toBe('2026-07-01')
        expect(requestUrl.searchParams.get('requestedTo')).toBe('2026-07-30')
    })

    it('includes the processing work type for a seller', async () => {
        const fetchMock = vi.fn().mockResolvedValue(pageResponse())
        vi.stubGlobal('fetch', fetchMock)

        await getSettlementRequests({ workType: 'PROCESSING' })

        const [url] = fetchMock.mock.calls[0] as [string, RequestInit]
        expect(new URL(url, 'http://localhost').searchParams.get('workType'))
            .toBe('PROCESSING')
    })

    it('includes seller and requested date filters for an admin', async () => {
        const fetchMock = vi.fn().mockResolvedValue(pageResponse())
        vi.stubGlobal('fetch', fetchMock)

        await getAdminSettlementRequests({
            sellerKeyword: 'YMall Store',
            requestedFrom: '2026-07-01',
            requestedTo: '2026-07-30',
        })

        const [url] = fetchMock.mock.calls[0] as [string, RequestInit]
        const requestUrl = new URL(url, 'http://localhost')
        expect(requestUrl.pathname).toBe('/api/admin/settlement-requests')
        expect(requestUrl.searchParams.get('sellerKeyword')).toBe('YMall Store')
        expect(requestUrl.searchParams.get('requestedFrom')).toBe('2026-07-01')
        expect(requestUrl.searchParams.get('requestedTo')).toBe('2026-07-30')
    })

    it('includes the action-required work type for an admin', async () => {
        const fetchMock = vi.fn().mockResolvedValue(pageResponse())
        vi.stubGlobal('fetch', fetchMock)

        await getAdminSettlementRequests({ workType: 'ACTION_REQUIRED' })

        const [url] = fetchMock.mock.calls[0] as [string, RequestInit]
        expect(new URL(url, 'http://localhost').searchParams.get('workType'))
            .toBe('ACTION_REQUIRED')
    })
})
