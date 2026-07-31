import type {
    ProductQuestion,
    ProductQuestionPage,
    ProductQuestionRequest,
    ProductQuestionStatus,
} from '../types/productQuestion'
import { apiRequest } from './client'

export function getProductQuestions(
    productId: number,
    page = 1,
    size = 10,
    signal?: AbortSignal,
) {
    return apiRequest<ProductQuestionPage>(
        `/products/${productId}/questions?page=${page}&size=${size}`,
        { signal },
    )
}

export function createProductQuestion(
    productId: number,
    request: ProductQuestionRequest,
) {
    return apiRequest<ProductQuestion>(
        `/products/${productId}/questions`,
        { method: 'POST', body: request },
    )
}

export function updateProductQuestion(
    questionId: number,
    request: ProductQuestionRequest,
) {
    return apiRequest<ProductQuestion>(
        `/product-questions/${questionId}`,
        { method: 'PUT', body: request },
    )
}

export function deleteProductQuestion(questionId: number) {
    return apiRequest<void>(
        `/product-questions/${questionId}`,
        { method: 'DELETE' },
    )
}

export function getSellerProductQuestions({
    page = 1,
    size = 20,
    status,
    keyword = '',
    signal,
}: {
    page?: number
    size?: number
    status?: ProductQuestionStatus
    keyword?: string
    signal?: AbortSignal
} = {}) {
    const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        keyword,
    })
    if (status) params.set('status', status)
    return apiRequest<ProductQuestionPage>(
        `/seller/product-questions?${params.toString()}`,
        { signal },
    )
}

export function getSellerProductQuestion(
    questionId: number,
    signal?: AbortSignal,
) {
    return apiRequest<ProductQuestion>(
        `/seller/product-questions/${questionId}`,
        { signal },
    )
}

export function getSellerPendingQuestionCount(signal?: AbortSignal) {
    return apiRequest<{ count: number }>(
        '/seller/product-questions/pending-count',
        { signal },
    )
}

export function saveSellerProductQuestionAnswer(
    questionId: number,
    content: string,
) {
    return apiRequest<ProductQuestion>(
        `/seller/product-questions/${questionId}/answer`,
        { method: 'PUT', body: { content } },
    )
}

export const SELLER_QUESTION_COUNT_CHANGED_EVENT =
    'ymall:seller-question-count-changed'

export function notifySellerQuestionCountChanged() {
    window.dispatchEvent(new Event(SELLER_QUESTION_COUNT_CHANGED_EVENT))
}
