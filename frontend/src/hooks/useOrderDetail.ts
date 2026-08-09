import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../api/client'
import {
    createReturnRequest,
    getOrder,
    getRefunds,
    getReturnRequests,
    requestRefund,
} from '../api/orders'
import { createReview, deleteReview, getAllMyReviews, updateReview } from '../api/reviews'
import { useToast } from '../toast/useToast'
import type {
    Order,
    OrderItem,
    PaymentRefund,
    PaymentRefundRequest,
    ReturnRequest,
    ReturnRequestCreateRequest,
} from '../types/order'
import type { Review } from '../types/review'

export interface ReviewEditorState {
    orderItemId: number
    reviewId?: number
    rating: number
    content: string
}

export function useOrderDetail(orderId: number, isValidOrderId: boolean) {
    const { showToast } = useToast()
    const [order, setOrder] = useState<Order | null>(null)
    const [reviews, setReviews] = useState<Review[]>([])
    const [editor, setEditor] = useState<ReviewEditorState | null>(null)
    const [reviewToDelete, setReviewToDelete] = useState<Review | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [isSavingReview, setIsSavingReview] = useState(false)
    const [errorMessage, setErrorMessage] = useState('')
    const [retryKey, setRetryKey] = useState(0)
    const [refundDialogOpen, setRefundDialogOpen] = useState(false)
    const [refunds, setRefunds] = useState<PaymentRefund[]>([])
    const [isLoadingRefunds, setIsLoadingRefunds] = useState(false)
    const [isRefunding, setIsRefunding] = useState(false)
    const [refundError, setRefundError] = useState('')
    const [returnRequests, setReturnRequests] = useState<ReturnRequest[]>([])
    const [returnItem, setReturnItem] = useState<OrderItem | null>(null)
    const [returnError, setReturnError] = useState('')
    const [isRequestingReturn, setIsRequestingReturn] = useState(false)
    const reviewsByOrderItemId = useMemo(
        () => new Map(reviews.map((review) => [review.orderItemId, review])),
        [reviews],
    )

    useEffect(() => {
        if (!isValidOrderId) return
        const controller = new AbortController()
        Promise.all([
            getOrder(orderId, controller.signal),
            getAllMyReviews(controller.signal),
            getReturnRequests(orderId, controller.signal),
        ])
            .then(([orderResponse, reviewResponse, returnResponse]) => {
                setOrder(orderResponse)
                setReviews(reviewResponse)
                setReturnRequests(returnResponse)
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError ? error.message : '주문 상세를 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [isValidOrderId, orderId, retryKey])

    function retry() {
        setErrorMessage('')
        setIsLoading(true)
        setRetryKey((value) => value + 1)
    }

    function openReviewEditor(item: OrderItem, review?: Review) {
        setEditor({
            orderItemId: item.orderItemId,
            reviewId: review?.reviewId,
            rating: review?.rating ?? 5,
            content: review?.content ?? '',
        })
    }

    function updateEditor(patch: Partial<Pick<ReviewEditorState, 'rating' | 'content'>>) {
        setEditor((current) => current ? { ...current, ...patch } : current)
    }

    async function saveReview(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        if (!editor || !editor.content.trim()) return
        setIsSavingReview(true)
        try {
            const savedReview = editor.reviewId
                ? await updateReview(editor.reviewId, {
                    rating: editor.rating,
                    content: editor.content.trim(),
                })
                : await createReview({
                    orderItemId: editor.orderItemId,
                    rating: editor.rating,
                    content: editor.content.trim(),
                })
            setReviews((current) => editor.reviewId
                ? current.map((review) => review.reviewId === savedReview.reviewId ? savedReview : review)
                : [savedReview, ...current])
            showToast(editor.reviewId ? '리뷰가 수정되었습니다.' : '리뷰가 등록되었습니다.', 'success')
            setEditor(null)
        } catch (error) {
            showToast(error instanceof ApiError ? error.message : '리뷰를 저장하지 못했습니다.', 'error')
        } finally {
            setIsSavingReview(false)
        }
    }

    async function removeSelectedReview() {
        if (!reviewToDelete) return
        setIsSavingReview(true)
        try {
            await deleteReview(reviewToDelete.reviewId)
            setReviews((current) => current.filter(
                (review) => review.reviewId !== reviewToDelete.reviewId,
            ))
            if (editor?.reviewId === reviewToDelete.reviewId) setEditor(null)
            setReviewToDelete(null)
            showToast('리뷰가 삭제되었습니다.', 'success')
        } catch (error) {
            showToast(error instanceof ApiError ? error.message : '리뷰를 삭제하지 못했습니다.', 'error')
        } finally {
            setIsSavingReview(false)
        }
    }

    async function openRefundDialog() {
        if (!order) return
        setRefundDialogOpen(true)
        setRefunds([])
        setRefundError('')
        setIsLoadingRefunds(true)
        try {
            setRefunds(await getRefunds(order.orderId))
        } catch (error) {
            setRefundError(error instanceof ApiError ? error.message : '환불 내역을 불러오지 못했습니다.')
        } finally {
            setIsLoadingRefunds(false)
        }
    }

    async function submitRefund(request: PaymentRefundRequest) {
        if (!order) return false
        setRefundError('')
        setIsRefunding(true)
        try {
            await requestRefund(order.orderId, request)
            const [updatedOrder, updatedRefunds] = await Promise.all([
                getOrder(order.orderId),
                getRefunds(order.orderId),
            ])
            setOrder(updatedOrder)
            setRefunds(updatedRefunds)
            showToast('환불 요청이 처리되었습니다.', 'success')
            return true
        } catch (error) {
            setRefundError(error instanceof ApiError ? error.message : '환불 요청을 처리하지 못했습니다.')
            return false
        } finally {
            setIsRefunding(false)
        }
    }

    function openReturnDialog(item: OrderItem) {
        setReturnError('')
        setReturnItem(item)
    }

    async function submitReturnRequest(request: ReturnRequestCreateRequest) {
        if (!order) return false
        setReturnError('')
        setIsRequestingReturn(true)
        try {
            const created = await createReturnRequest(order.orderId, request)
            setReturnRequests((current) => [created, ...current])
            showToast('반품을 신청했습니다.', 'success')
            return true
        } catch (error) {
            setReturnError(error instanceof ApiError ? error.message : '반품을 신청하지 못했습니다.')
            return false
        } finally {
            setIsRequestingReturn(false)
        }
    }

    return {
        closeEditor: () => setEditor(null),
        closeRefundDialog: () => {
            if (!isRefunding) setRefundDialogOpen(false)
        },
        closeReturnDialog: () => {
            if (!isRequestingReturn) setReturnItem(null)
        },
        editor,
        errorMessage,
        isLoading,
        isLoadingRefunds,
        isRefunding,
        isRequestingReturn,
        isSavingReview,
        openRefundDialog,
        openReturnDialog,
        openReviewEditor,
        refundDialogOpen,
        refundError,
        refunds,
        removeSelectedReview,
        retry,
        returnError,
        returnItem,
        returnRequests,
        reviewToDelete,
        reviewsByOrderItemId,
        saveReview,
        setReviewToDelete,
        submitRefund,
        submitReturnRequest,
        updateEditor,
        order,
    }
}
