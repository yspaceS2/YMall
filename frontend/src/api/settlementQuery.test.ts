import { describe, expect, it } from 'vitest'

import { buildSettlementRequestQuery } from './settlementQuery'

describe('buildSettlementRequestQuery', () => {
    it('includes pagination and supplied filters in a stable order', () => {
        expect(buildSettlementRequestQuery({
            page: 2,
            size: 30,
            status: 'REQUESTED',
            workType: 'REVIEW_REQUIRED',
            requestId: 15,
            sellerKeyword: '  seller  ',
            requestedFrom: '2026-08-01',
            requestedTo: '2026-08-08',
        })).toBe(
            'page=2&size=30&status=REQUESTED&workType=REVIEW_REQUIRED'
            + '&requestId=15&sellerKeyword=seller&requestedFrom=2026-08-01'
            + '&requestedTo=2026-08-08',
        )
    })

    it('uses pagination defaults and omits empty optional filters', () => {
        expect(buildSettlementRequestQuery({ sellerKeyword: '   ' }))
            .toBe('page=1&size=20')
    })
})
