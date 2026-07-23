const CUSTOMER_KEY_STORAGE_PREFIX = 'ymall.toss.customerKey.'
const IDEMPOTENCY_KEY_PREFIX = 'ymall.toss.confirmation.'

export function getTossCustomerKey(memberKey: string) {
    const storageKey = `${CUSTOMER_KEY_STORAGE_PREFIX}${memberKey}`
    const savedKey = localStorage.getItem(storageKey)
    if (savedKey) return savedKey

    const customerKey = crypto.randomUUID()
    localStorage.setItem(storageKey, customerKey)
    return customerKey
}

export function getConfirmationIdempotencyKey(paymentKey: string) {
    const storageKey = `${IDEMPOTENCY_KEY_PREFIX}${paymentKey}`
    const savedKey = sessionStorage.getItem(storageKey)
    if (savedKey) return savedKey

    const idempotencyKey = crypto.randomUUID()
    sessionStorage.setItem(storageKey, idempotencyKey)
    return idempotencyKey
}

export function getTossErrorMessage(error: unknown) {
    if (
        typeof error === 'object'
        && error !== null
        && 'message' in error
        && typeof error.message === 'string'
    ) {
        return error.message
    }
    return '결제창을 열지 못했습니다. 잠시 후 다시 시도해 주세요.'
}
