export interface SettlementRequestQueryOptions {
    page?: number
    size?: number
    status?: string
    workType?: string
    requestId?: number
    sellerKeyword?: string
    requestedFrom?: string
    requestedTo?: string
}

export function buildSettlementRequestQuery({
    page = 1,
    size = 20,
    status,
    workType,
    requestId,
    sellerKeyword,
    requestedFrom,
    requestedTo,
}: SettlementRequestQueryOptions = {}) {
    const query = new URLSearchParams({
        page: String(page),
        size: String(size),
    })
    if (status) query.set('status', status)
    if (workType) query.set('workType', workType)
    if (requestId !== undefined) query.set('requestId', String(requestId))
    if (sellerKeyword?.trim()) query.set('sellerKeyword', sellerKeyword.trim())
    if (requestedFrom) query.set('requestedFrom', requestedFrom)
    if (requestedTo) query.set('requestedTo', requestedTo)
    return query.toString()
}
