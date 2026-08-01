import { afterEach, describe, expect, it, vi } from 'vitest'
import { getProductSuggestions } from './products'

describe('product search api', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
    })

    it('추천 검색어 요청에 한글 초성과 개수 제한을 전달한다', async () => {
        const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
            success: true,
            data: [],
            message: null,
        }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
        }))
        vi.stubGlobal('fetch', fetchMock)

        await getProductSuggestions('ㄴ ㅌ ㅂ', 8)

        expect(fetchMock).toHaveBeenCalledWith(
            '/api/products/suggestions?keyword=%E3%84%B4+%E3%85%8C+%E3%85%82&size=8',
            expect.objectContaining({ credentials: 'include' }),
        )
    })

    it('카테고리 추천 검색어 요청에는 카테고리 ID를 전달한다', async () => {
        const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
            success: true,
            data: [],
            message: null,
        }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
        }))
        vi.stubGlobal('fetch', fetchMock)

        await getProductSuggestions('재킷', 8, 2)

        expect(fetchMock).toHaveBeenCalledWith(
            '/api/products/suggestions?keyword=%EC%9E%AC%ED%82%B7&size=8&categoryId=2',
            expect.objectContaining({ credentials: 'include' }),
        )
    })
})
