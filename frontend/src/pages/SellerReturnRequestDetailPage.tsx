import { ArrowLeft } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import {
    approveSellerReturnRequest,
    getSellerReturnRequest,
    rejectSellerReturnRequest,
} from '../api/seller'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { PageState } from '../components/ui/PageState'
import type { ReturnRequest } from '../types/order'
import { formatKoreanDateTime } from '../utils/dateTime'
import {
    SellerReturnManagementPage,
} from './SellerReturnRequestUi'
import { returnStatusLabel } from './sellerReturnStatus'

export function SellerReturnRequestDetailPage() {
    const { returnRequestId } = useParams()
    const parsedRequestId = Number(returnRequestId)
    const isValidRequestId = Number.isInteger(parsedRequestId) && parsedRequestId > 0
    const [request, setRequest] = useState<ReturnRequest | null>(null)
    const [response, setResponse] = useState('')
    const [isLoading, setIsLoading] = useState(isValidRequestId)
    const [isProcessing, setIsProcessing] = useState(false)
    const [errorMessage, setErrorMessage] = useState('')
    const [successMessage, setSuccessMessage] = useState('')

    useEffect(() => {
        if (!isValidRequestId) return
        const controller = new AbortController()
        getSellerReturnRequest(parsedRequestId, controller.signal)
            .then((result) => {
                setRequest(result)
                setResponse(result.sellerResponse ?? '')
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(
                    error instanceof ApiError
                        ? error.message
                        : '반품 요청 상세를 불러오지 못했습니다.',
                )
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [isValidRequestId, parsedRequestId])

    async function processRequest(action: 'approve' | 'reject') {
        if (!request || isProcessing) return
        const normalizedResponse = response.trim()
            || (action === 'approve' ? '반품 승인 및 환불 처리 완료' : '')
        if (!normalizedResponse) {
            setErrorMessage('거절 사유를 입력해 주세요.')
            return
        }

        setErrorMessage('')
        setSuccessMessage('')
        setIsProcessing(true)
        try {
            const updated = action === 'approve'
                ? await approveSellerReturnRequest(
                    request.returnRequestId,
                    normalizedResponse,
                )
                : await rejectSellerReturnRequest(
                    request.returnRequestId,
                    normalizedResponse,
                )
            setRequest(updated)
            setResponse(updated.sellerResponse ?? '')
            setSuccessMessage(
                action === 'approve'
                    ? '반품 승인과 환불 처리가 완료되었습니다.'
                    : '반품 요청을 거절했습니다.',
            )
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError
                    ? error.message
                    : '반품 요청을 처리하지 못했습니다.',
            )
        } finally {
            setIsProcessing(false)
        }
    }

    if (!isValidRequestId) {
        return (
            <SellerReturnManagementPage eyebrow="RETURN DETAIL" title="반품 상세">
                <FeedbackMessage tone="error">
                    올바르지 않은 반품 요청번호입니다.
                </FeedbackMessage>
            </SellerReturnManagementPage>
        )
    }
    if (isLoading) {
        return <PageState variant="loading" title="반품 요청을 불러오는 중입니다" />
    }

    return (
        <SellerReturnManagementPage
            eyebrow="RETURN DETAIL"
            title={`반품 요청 #${returnRequestId}`}
            description="구매자의 반품 사유와 처리 결과를 확인합니다."
        >
            <Link
                className="mb-6 inline-flex items-center gap-2 text-xs font-bold text-muted hover:text-ink"
                to="/seller/returns"
            >
                <ArrowLeft className="size-4" />
                반품 목록
            </Link>
            {errorMessage && (
                <FeedbackMessage className="mb-5" tone="error">
                    {errorMessage}
                </FeedbackMessage>
            )}
            {successMessage && (
                <FeedbackMessage className="mb-5" tone="success">
                    {successMessage}
                </FeedbackMessage>
            )}
            {request && (
                <div className="grid gap-6 border-t-2 border-ink pt-6">
                    <dl className="grid gap-4 bg-surface p-5 min-[701px]:grid-cols-3">
                        <Detail label="주문번호" value={`#${request.orderId}`} />
                        <Detail label="처리 상태" value={returnStatusLabel[request.status]} />
                        <Detail label="요청일" value={formatKoreanDateTime(request.requestedAt)} />
                        <Detail label="상품" value={request.productName} />
                        <Detail label="구매자" value={request.memberName} />
                        <Detail label="반품 수량" value={`${request.quantity}개`} />
                    </dl>
                    <section className="border border-line bg-paper p-5">
                        <h2 className="text-sm font-bold">구매자 반품 사유</h2>
                        <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-muted">
                            {request.reason}
                        </p>
                    </section>

                    {request.status === 'REQUESTED' ? (
                        <section className="grid gap-4 border border-line bg-surface p-5">
                            <label className="grid gap-2 text-sm font-bold">
                                판매자 답변
                                <textarea
                                    className="min-h-28 resize-y border border-line bg-paper p-3 text-sm font-normal text-ink"
                                    maxLength={500}
                                    value={response}
                                    placeholder="승인 안내 또는 거절 사유를 입력해 주세요."
                                    disabled={isProcessing}
                                    onChange={(event) => setResponse(event.target.value)}
                                />
                            </label>
                            <div className="flex justify-end gap-2">
                                <button
                                    className="h-11 border border-danger px-5 text-xs font-bold text-danger disabled:opacity-50"
                                    type="button"
                                    disabled={isProcessing}
                                    onClick={() => void processRequest('reject')}
                                >
                                    거절
                                </button>
                                <button
                                    className="h-11 bg-ink px-5 text-xs font-bold text-white disabled:opacity-50"
                                    type="button"
                                    disabled={isProcessing}
                                    onClick={() => void processRequest('approve')}
                                >
                                    {isProcessing ? '처리 중...' : '승인 및 환불'}
                                </button>
                            </div>
                        </section>
                    ) : (
                        <section className="border border-line bg-surface p-5">
                            <h2 className="text-sm font-bold">처리 결과</h2>
                            <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-muted">
                                {request.sellerResponse || '등록된 답변이 없습니다.'}
                            </p>
                        </section>
                    )}
                </div>
            )}
        </SellerReturnManagementPage>
    )
}

function Detail({ label, value }: { label: string; value: string }) {
    return (
        <div>
            <dt className="text-xs text-muted">{label}</dt>
            <dd className="mt-1 text-sm font-bold">{value}</dd>
        </div>
    )
}
