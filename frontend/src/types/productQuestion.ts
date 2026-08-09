import type { PageResponse } from './api'

export type ProductQuestionStatus = 'WAITING' | 'ANSWERED'

export interface ProductQuestionAnswer {
    answerId: number
    content: string
    createdAt: string
    updatedAt: string
}

export interface ProductQuestion {
    questionId: number
    productId: number
    productName: string
    thumbnailUrl: string | null
    memberName: string
    title: string
    content: string | null
    privateQuestion: boolean
    ownedByRequester: boolean
    contentVisible: boolean
    status: ProductQuestionStatus
    answer: ProductQuestionAnswer | null
    createdAt: string
    updatedAt: string
}

export interface ProductQuestionRequest {
    title: string
    content: string
    privateQuestion: boolean
}

export type ProductQuestionPage = PageResponse<ProductQuestion>
