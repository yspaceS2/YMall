const CUSTOMER_KEY_STORAGE_PREFIX = 'ymall.toss.customerKey.'
const IDEMPOTENCY_KEY_PREFIX = 'ymall.toss.confirmation.'

/**
 * 로그인 회원에 대해 브라우저에서 유지되는 Toss 고객 키를 반환한다.
 *
 * 이 값은 인증 자격 증명이 아닌 SDK용 불투명 식별자이다. localStorage에 보관하여 이후 결제에서도
 * Toss가 같은 브라우저의 고객을 식별할 수 있게 한다.
 */
export function getTossCustomerKey(memberKey: string) {
    const storageKey = `${CUSTOMER_KEY_STORAGE_PREFIX}${memberKey}`
    const savedKey = localStorage.getItem(storageKey)
    if (savedKey) return savedKey

    const customerKey = crypto.randomUUID()
    localStorage.setItem(storageKey, customerKey)
    return customerKey
}

/**
 * 현재 탭에서 같은 결제를 재시도할 때 동일한 승인 멱등성 키를 반환한다.
 *
 * 결제 키가 다르면 새 값을 사용하고 같은 결제의 네트워크 재시도에는 기존 값을 재사용한다.
 * 재사용 범위를 현재 브라우저 탭 세션으로 제한하기 위해 sessionStorage에 보관한다.
 */
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
