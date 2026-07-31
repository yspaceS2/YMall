import { afterEach, describe, expect, it, vi } from 'vitest'
import { uploadProductImage } from './files'

describe('uploadProductImage', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
    })

    it('sends the selected file as multipart form data', async () => {
        const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
            success: true,
            data: {
                originalFileName: 'product.jpg',
                storedFileName: 'stored.jpg',
                fileUrl: '/images/public/products/2026/07/31/stored.jpg',
                thumbnailFileName: 'thumb-stored.jpg',
                thumbnailUrl: '/images/public/products/2026/07/31/thumb-stored.jpg',
                size: 5,
                contentType: 'image/jpeg',
            },
            message: 'uploaded',
        }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
        }))
        vi.stubGlobal('fetch', fetchMock)
        const file = new File(['image'], 'product.jpg', { type: 'image/jpeg' })

        await uploadProductImage(file)

        const [, options] = fetchMock.mock.calls[0] as [string, RequestInit]
        expect(options.body).toBeInstanceOf(FormData)
        expect(new Headers(options.headers).has('Content-Type')).toBe(false)
        expect((options.body as FormData).get('file')).toBe(file)
    })
})
