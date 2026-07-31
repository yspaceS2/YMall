import { afterEach, describe, expect, it, vi } from 'vitest'

import {
    getSellerOrders,
    getSellerPendingOrderCount,
    getSellerProducts,
} from './seller'

function successResponse(data: unknown) {
    return new Response(JSON.stringify({
        success: true,
        data,
        message: 'ok',
    }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
    })
}

function pageResponse() {
    return {
        content: [],
        page: 1,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
    }
}

describe('seller list filters', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
    })

    it('상품 검색·카테고리·재고 조건을 쿼리에 포함한다', async () => {
        const fetchMock = vi.fn().mockResolvedValue(successResponse(pageResponse()))
        vi.stubGlobal('fetch', fetchMock)

        await getSellerProducts({
            page: 2,
            size: 10,
            keyword: '노트북',
            categoryId: 12,
            stockCondition: 'LTE',
            stockQuantity: 5,
        })

        const [url] = fetchMock.mock.calls[0] as [string, RequestInit]
        const requestUrl = new URL(url, 'http://localhost')
        expect(requestUrl.pathname).toBe('/api/seller/products')
        expect(requestUrl.searchParams.get('page')).toBe('2')
        expect(requestUrl.searchParams.get('size')).toBe('10')
        expect(requestUrl.searchParams.get('keyword')).toBe('노트북')
        expect(requestUrl.searchParams.get('categoryId')).toBe('12')
        expect(requestUrl.searchParams.get('stockCondition')).toBe('LTE')
        expect(requestUrl.searchParams.get('stockQuantity')).toBe('5')
    })

    it('주문 검색어와 배송 상태를 쿼리에 포함한다', async () => {
        const fetchMock = vi.fn().mockResolvedValue(successResponse(pageResponse()))
        vi.stubGlobal('fetch', fetchMock)

        await getSellerOrders({
            keyword: '12073',
            fulfillmentStatus: 'PENDING',
        })

        const [url] = fetchMock.mock.calls[0] as [string, RequestInit]
        const requestUrl = new URL(url, 'http://localhost')
        expect(requestUrl.pathname).toBe('/api/seller/orders')
        expect(requestUrl.searchParams.get('keyword')).toBe('12073')
        expect(requestUrl.searchParams.get('fulfillmentStatus')).toBe('PENDING')
    })

    it('처리 대기 주문 상품 수 API를 호출한다', async () => {
        const fetchMock = vi.fn().mockResolvedValue(successResponse({ count: 2 }))
        vi.stubGlobal('fetch', fetchMock)

        await expect(getSellerPendingOrderCount()).resolves.toEqual({ count: 2 })

        const [url] = fetchMock.mock.calls[0] as [string, RequestInit]
        expect(new URL(url, 'http://localhost').pathname)
            .toBe('/api/seller/orders/pending-count')
    })
})
