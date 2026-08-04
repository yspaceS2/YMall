import { LoaderCircle } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { getSellerReturnRequests } from '../api/seller'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { PageState } from '../components/ui/PageState'
import type { ReturnRequest, ReturnRequestStatus } from '../types/order'
import { formatKoreanDateTime } from '../utils/dateTime'
import { resolveImageUrl } from '../utils/product'
import {
    ReturnStatusBadge,
    SellerReturnManagementPage,
} from './SellerReturnRequestUi'

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
        <SellerReturnManagementPage
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
        </SellerReturnManagementPage>
    )
}

function parseReturnStatus(value: string | null): ReturnRequestStatus | undefined {
    return value === 'REQUESTED'
        || value === 'APPROVED'
        || value === 'REJECTED'
        ? value
        : undefined
}
