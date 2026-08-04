import { ArrowLeft, LoaderCircle } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import {
    approveSellerReturnRequest,
    getSellerReturnRequest,
    getSellerReturnRequests,
    rejectSellerReturnRequest,
} from '../api/seller'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { PageState } from '../components/ui/PageState'
import { StatusBadge, type StatusBadgeTone } from '../components/ui/StatusBadge'
import type { ReturnRequest, ReturnRequestStatus } from '../types/order'
import { formatKoreanDateTime } from '../utils/dateTime'
import { resolveImageUrl } from '../utils/product'

const PAGE_SIZE = 20

export function SellerReturnRequestsPage() {
    const navigate = useNavigate()
    const [searchParams, setSearchParams] = useSearchParams()
    const page = Math.max(Number(searchParams.get('page')) || 1, 1)
    const keyword = searchParams.get('keyword') ?? ''
    const status = parseReturnStatus(searchParams.get('status'))
    const [keywordInput, setKeywordInput] = useState(keyword)
    const [requests, setRequests] = useState<ReturnRequest[]>([])
    const [totalPages, setTotalPages] = useState(0)
    const [totalElements, setTotalElements] = useState(0)
    const [isLoading, setIsLoading] = useState(true)
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        getSellerReturnRequests({
            page,
            size: PAGE_SIZE,
            status,
            keyword,
            signal: controller.signal,
        })
            .then((response) => {
                setRequests(response.content)
                setTotalPages(response.totalPages)
                setTotalElements(response.totalElements)
                setErrorMessage('')
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(
                    error instanceof ApiError
                        ? error.message
                        : '반품 요청을 불러오지 못했습니다.',
                )
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [keyword, page, status])

    function updateParams(nextValues: {
        page?: number
        status?: ReturnRequestStatus | ''
        keyword?: string
    }) {
        setIsLoading(true)
        const next = new URLSearchParams(searchParams)
        if (nextValues.page !== undefined) next.set('page', String(nextValues.page))
        if (nextValues.status !== undefined) {
            if (nextValues.status) next.set('status', nextValues.status)
            else next.delete('status')
        }
        if (nextValues.keyword !== undefined) {
            if (nextValues.keyword) next.set('keyword', nextValues.keyword)
            else next.delete('keyword')
        }
        setSearchParams(next)
    }

    function search(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        updateParams({ page: 1, keyword: keywordInput.trim() })
    }

    return (
        <ManagementPage
            eyebrow="RETURN MANAGEMENT"
            title="반품 관리"
            description={`반품 요청 ${totalElements.toLocaleString()}건`}
        >
            <div className="mb-5 grid gap-3 min-[701px]:grid-cols-[200px_1fr]">
                <label className="grid gap-1.5 text-xs font-bold">
                    처리 상태
                    <select
                        className="h-11 border border-line bg-surface px-3 text-sm font-normal text-ink"
                        value={status ?? ''}
                        onChange={(event) => updateParams({
                            page: 1,
                            status: parseReturnStatus(event.target.value) ?? '',
                        })}
                    >
                        <option value="">전체 상태</option>
                        <option value="REQUESTED">처리 대기</option>
                        <option value="APPROVED">승인·환불 완료</option>
                        <option value="REJECTED">거절</option>
                    </select>
                </label>
                <form className="flex items-end gap-2" onSubmit={search}>
                    <label className="grid flex-1 gap-1.5 text-xs font-bold">
                        검색
                        <input
                            className="h-11 border border-line bg-surface px-3 text-sm font-normal text-ink"
                            value={keywordInput}
                            placeholder="상품명 또는 구매자명"
                            onChange={(event) => setKeywordInput(event.target.value)}
                        />
                    </label>
                    <button className="h-11 bg-ink px-5 text-xs font-bold text-white" type="submit">
                        검색
                    </button>
                </form>
            </div>

            {errorMessage && (
                <FeedbackMessage className="mb-5" tone="error">
                    {errorMessage}
                </FeedbackMessage>
            )}
            {isLoading ? (
                <div className="grid min-h-60 place-content-center">
                    <LoaderCircle className="size-6 animate-spin" />
                </div>
            ) : requests.length === 0 ? (
                <PageState
                    variant="empty"
                    title="조건에 맞는 반품 요청이 없습니다"
                    description="새 반품 요청이 접수되면 이곳에서 확인할 수 있습니다."
                />
            ) : (
                <div className="overflow-x-auto border-t-2 border-ink">
                    <table className="w-full min-w-190 text-left text-sm">
                        <thead className="border-b border-line bg-surface text-xs">
                            <tr>
                                <th className="p-4">상품</th>
                                <th className="p-4">구매자</th>
                                <th className="p-4">수량</th>
                                <th className="p-4">상태</th>
                                <th className="p-4">요청일</th>
                            </tr>
                        </thead>
                        <tbody>
                            {requests.map((request) => (
                                <tr
                                    className="cursor-pointer border-b border-line transition-colors hover:bg-surface"
                                    key={request.returnRequestId}
                                    onClick={() => navigate(
                                        `/seller/returns/${request.returnRequestId}`,
                                    )}
                                >
                                    <td className="p-4">
                                        <div className="flex min-w-72 items-center gap-3">
                                            <div className="grid size-14 shrink-0 place-items-center overflow-hidden border border-line bg-surface text-[9px] font-bold text-muted">
                                                {request.thumbnailUrl ? (
                                                    <img
                                                        alt=""
                                                        className="size-full object-cover"
                                                        src={resolveImageUrl(request.thumbnailUrl)}
                                                    />
                                                ) : 'YMALL'}
                                            </div>
                                            <strong className="truncate">
                                                {request.productName}
                                            </strong>
                                        </div>
                                    </td>
                                    <td className="p-4">{request.memberName}</td>
                                    <td className="p-4">{request.quantity}개</td>
                                    <td className="p-4">
                                        <ReturnStatusBadge status={request.status} />
                                    </td>
                                    <td className="p-4">
                                        {formatKoreanDateTime(request.requestedAt)}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {totalPages > 1 && (
                <div className="mt-7 flex justify-center gap-2">
                    <button
                        className="h-10 border border-line px-4 text-xs font-bold disabled:opacity-40"
                        type="button"
                        disabled={page <= 1}
                        onClick={() => updateParams({ page: page - 1 })}
                    >
                        이전
                    </button>
                    <span className="grid h-10 min-w-20 place-items-center text-xs font-bold">
                        {page} / {totalPages}
                    </span>
                    <button
                        className="h-10 border border-line px-4 text-xs font-bold disabled:opacity-40"
                        type="button"
                        disabled={page >= totalPages}
                        onClick={() => updateParams({ page: page + 1 })}
                    >
                        다음
                    </button>
                </div>
            )}
        </ManagementPage>
    )
}

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
            <ManagementPage eyebrow="RETURN DETAIL" title="반품 상세">
                <FeedbackMessage tone="error">
                    올바르지 않은 반품 요청번호입니다.
                </FeedbackMessage>
            </ManagementPage>
        )
    }
    if (isLoading) {
        return <PageState variant="loading" title="반품 요청을 불러오는 중입니다" />
    }

    return (
        <ManagementPage
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
        </ManagementPage>
    )
}

function ManagementPage({
    eyebrow,
    title,
    description,
    children,
}: {
    eyebrow: string
    title: string
    description?: string
    children: React.ReactNode
}) {
    return (
        <section className="mx-auto max-w-350 px-4 py-10 min-[601px]:px-8 min-[601px]:py-14">
            <p className="text-[10px] font-extrabold tracking-[.18em] text-accent">
                {eyebrow}
            </p>
            <div className="mb-8 mt-2">
                <h1 className="font-serif text-[clamp(34px,5vw,54px)] leading-none tracking-tighter">
                    {title}
                </h1>
                {description && <p className="mt-3 text-sm text-muted">{description}</p>}
            </div>
            {children}
        </section>
    )
}

function ReturnStatusBadge({ status }: { status: ReturnRequestStatus }) {
    const tones: Record<ReturnRequestStatus, StatusBadgeTone> = {
        REQUESTED: 'warning',
        APPROVED: 'success',
        REJECTED: 'danger',
    }
    return (
        <StatusBadge tone={tones[status]}>
            {returnStatusLabel[status]}
        </StatusBadge>
    )
}

const returnStatusLabel: Record<ReturnRequestStatus, string> = {
    REQUESTED: '처리 대기',
    APPROVED: '승인·환불 완료',
    REJECTED: '거절',
}

function parseReturnStatus(value: string | null): ReturnRequestStatus | undefined {
    return value === 'REQUESTED'
        || value === 'APPROVED'
        || value === 'REJECTED'
        ? value
        : undefined
}

function Detail({ label, value }: { label: string; value: string }) {
    return (
        <div>
            <dt className="text-xs text-muted">{label}</dt>
            <dd className="mt-1 text-sm font-bold">{value}</dd>
        </div>
    )
}
