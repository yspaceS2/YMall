import { describe, expect, it } from 'vitest'
import { resolveLoginDestination } from './loginDestination'

describe('resolveLoginDestination', () => {
    it('allows a seller to return to a seller page after reauthentication', () => {
        expect(
            resolveLoginDestination('/seller/orders/12073', 'ROLE_SELLER'),
        ).toBe('/seller/orders/12073')
    })

    it('allows an admin to return to seller and admin pages', () => {
        expect(resolveLoginDestination('/seller/products', 'ROLE_ADMIN')).toBe(
            '/seller/products',
        )
        expect(resolveLoginDestination('/admin/members', 'ROLE_ADMIN')).toBe(
            '/admin/members',
        )
    })

    it('sends a user home instead of restoring a seller or admin page', () => {
        expect(resolveLoginDestination('/seller/orders', 'ROLE_USER')).toBe('/')
        expect(resolveLoginDestination('/admin/members', 'ROLE_USER')).toBe('/')
    })

    it('sends a seller home instead of restoring an admin page', () => {
        expect(resolveLoginDestination('/admin/settlement', 'ROLE_SELLER')).toBe('/')
    })

    it('rejects external-looking, forbidden, and role-less destinations', () => {
        expect(resolveLoginDestination('//example.com', 'ROLE_USER')).toBe('/')
        expect(resolveLoginDestination('/forbidden', 'ROLE_USER')).toBe('/')
        expect(resolveLoginDestination('/mypage/orders', null)).toBe('/')
    })
})
