import { LoaderCircle } from 'lucide-react'
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

import { getAdminSettlementRequests } from '../../api/admin'
import { getApiErrorMessage, isAbortError } from '../../api/errors'
import { getSettlementRequests } from '../../api/seller'
import type {
    SettlementRequest,
    SettlementRequestStatus,
    SettlementRequestWorkType,
} from '../../types/seller'
import {
    ManagementEmpty,
    ManagementPagination,
} from '../management/ManagementListUi'
import { formatKoreanDateTime } from '../../utils/dateTime'
import { formatPrice } from '../../utils/product'
import {
    settlementStatuses,
    settlementStatusLabel,
    settlementStatusTone,
} from './settlementStatus'
import { StatusBadge } from '../ui/StatusBadge'

export function SellerSettlementRequestList() {
    return <SettlementRequestList role="seller" />
}
export function AdminSettlementRequestList() {
    return <SettlementRequestList role="admin" />
}

function SettlementRequestList({ role }: { role: 'seller' | 'admin' }) {
    const navigate = useNavigate()
    const [searchParams] = useSearchParams()
    const page = Math.max(Number(searchParams.get('page')) || 1, 1)
    const workType = parseWorkType(role, searchParams.get('workType'))
    const statusParam = searchParams.get('status')
    const status = !workType && settlementStatuses.includes(statusParam as SettlementRequestStatus)
        ? statusParam as SettlementRequestStatus
        : undefined
    const requestIdParam = searchParams.get('requestId')
    const requestId = requestIdParam && /^\d+$/.test(requestIdParam)
        ? Number(requestIdParam)
        : undefined
    const sellerKeyword = role === 'admin'
        ? searchParams.get('sellerKeyword')?.trim() || undefined
        : undefined
    const requestedFrom = searchParams.get('requestedFrom') || undefined
    const requestedTo = searchParams.get('requestedTo') || undefined
    const [requests, setRequests] = useState<SettlementRequest[]>([])
    const [pagination, setPagination] = useState({
        page,
        totalPages: 0,
        totalElements: 0,
    })
    const [isLoading, setIsLoading] = useState(true)
    const [errorMessage, setErrorMessage] = useState('')

    const options = useMemo(() => ({
        page,
        size: 20,
        status,
        workType,
        requestId,
        sellerKeyword,
        requestedFrom,
        requestedTo,
    }), [page, requestId, requestedFrom, requestedTo, sellerKeyword, status, workType])

    useEffect(() => {
        const controller = new AbortController()
        const load = role === 'admin'
            ? getAdminSettlementRequests({ ...options, signal: controller.signal })
            : getSettlementRequests({ ...options, signal: controller.signal })
        load
            .then((response) => {
                setErrorMessage('')
                setRequests(response.content)
                setPagination({
                    page: response.page,
                    totalPages: response.totalPages,
                    totalElements: response.totalElements,
                })
            })
            .catch((error: unknown) => {
                if (isAbortError(error)) return
                setErrorMessage(getApiErrorMessage(error, '정산 신청 목록을 불러오지 못했습니다.'))
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [options, role])

    const detailPath = (settlementRequestId: number) =>
        `/${role}/settlement/${settlementRequestId}`

    return (
        <section className="border-t-2 border-ink pt-5" aria-labelledby={`${role}-settlement-list-title`}>
            <h2 className="text-xl font-bold" id={`${role}-settlement-list-title`}>
                {role === 'admin' ? '정산 신청 관리' : '신청 이력'}
            </h2>
            <p className="mt-2 text-sm text-muted">
                상태, 신청일, 정산번호
                {role === 'admin' ? ', 판매자' : ''} 조건으로 조회합니다.
            </p>

            <SettlementListFilters role={role} />

            {errorMessage && (
                <p className="mb-4 border border-danger/35 bg-danger-soft p-3 text-sm text-danger" role="alert">
                    {errorMessage}
                </p>
            )}

            {isLoading ? (
                <div className="grid min-h-44 place-content-center">
                    <LoaderCircle className="size-5 animate-spin" aria-label="정산 목록 로딩 중" />
                </div>
            ) : requests.length === 0 ? (
                <ManagementEmpty>조건에 맞는 정산 신청이 없습니다.</ManagementEmpty>
            ) : (
                <>
                    <div className="overflow-x-auto border-y-2 border-ink">
                        <table className="w-full min-w-230 text-left text-sm">
                            <thead className="border-b border-ink bg-panel text-xs">
                                <tr>
                                    <th className="px-4 py-3">정산번호</th>
                                    {role === 'admin' && <th className="px-4 py-3">판매자</th>}
                                    <th className="px-4 py-3 text-right">매출</th>
                                    <th className="px-4 py-3 text-right">수수료</th>
                                    <th className="px-4 py-3 text-right">정산액</th>
                                    <th className="px-4 py-3">상태</th>
                                    <th className="px-4 py-3">신청일</th>
                                </tr>
                            </thead>
                            <tbody>
                                {requests.map((request) => (
                                    <tr
                                        className="cursor-pointer border-b border-line transition-colors last:border-b-0 hover:bg-panel focus-visible:bg-panel focus-visible:outline-2 focus-visible:outline-ink"
                                        key={request.settlementRequestId}
                                        tabIndex={0}
                                        onClick={() => navigate(detailPath(request.settlementRequestId))}
                                        onKeyDown={(event) => {
                                            if (event.key === 'Enter' || event.key === ' ') {
                                                event.preventDefault()
                                                navigate(detailPath(request.settlementRequestId))
                                            }
                                        }}
                                    >
                                        <td className="px-4 py-4 font-bold">#{request.settlementRequestId}</td>
                                        {role === 'admin' && <td className="px-4 py-4">{request.storeName}</td>}
                                        <td className="px-4 py-4 text-right">{formatPrice(request.grossAmount)}</td>
                                        <td className="px-4 py-4 text-right">{formatPrice(request.feeAmount)}</td>
                                        <td className="px-4 py-4 text-right font-bold">{formatPrice(request.settlementAmount)}</td>
                                        <td className="px-4 py-4">
                                            <StatusBadge tone={settlementStatusTone[request.status]}>
                                                {settlementStatusLabel[request.status]}
                                            </StatusBadge>
                                        </td>
                                        <td className="px-4 py-4 text-xs text-muted">
                                            {formatKoreanDateTime(request.createdAt)}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    <p className="mt-4 text-xs text-muted">총 {pagination.totalElements}건</p>
                    <ManagementPagination
                        page={pagination.page}
                        totalPages={pagination.totalPages}
                    />
                </>
            )}
        </section>
    )
}

function SettlementListFilters({ role }: { role: 'seller' | 'admin' }) {
    const [searchParams, setSearchParams] = useSearchParams()
    const [requestId, setRequestId] = useState(searchParams.get('requestId') ?? '')
    const [status, setStatus] = useState(
        searchParams.get('workType') ?? searchParams.get('status') ?? '',
    )
    const [sellerKeyword, setSellerKeyword] = useState(
        searchParams.get('sellerKeyword') ?? '',
    )
    const [requestedFrom, setRequestedFrom] = useState(
        searchParams.get('requestedFrom') ?? '',
    )
    const [requestedTo, setRequestedTo] = useState(
        searchParams.get('requestedTo') ?? '',
    )

    function submit(event: FormEvent) {
        event.preventDefault()
        const next = new URLSearchParams(searchParams)
        const normalized = requestId.trim()
        if (normalized) next.set('requestId', normalized)
        else next.delete('requestId')
        const normalizedSeller = sellerKeyword.trim()
        if (role === 'admin' && normalizedSeller) {
            next.set('sellerKeyword', normalizedSeller)
        } else {
            next.delete('sellerKeyword')
        }
        if (requestedFrom) next.set('requestedFrom', requestedFrom)
        else next.delete('requestedFrom')
        if (requestedTo) next.set('requestedTo', requestedTo)
        else next.delete('requestedTo')
        next.delete('status')
        next.delete('workType')
        if (status === 'PROCESSING' || status === 'ACTION_REQUIRED') {
            next.set('workType', status)
        } else if (status) {
            next.set('status', status)
        }
        next.set('page', '1')
        setSearchParams(next)
    }

    return (
        <form
            className={`my-6 grid gap-x-5 gap-y-3 min-[701px]:grid-cols-2 ${
                role === 'admin'
                    ? 'min-[1101px]:grid-cols-[minmax(140px,0.8fr)_minmax(160px,1fr)_minmax(150px,0.9fr)_minmax(320px,1.7fr)_96px]'
                    : 'min-[1101px]:grid-cols-[minmax(140px,0.8fr)_minmax(320px,1.7fr)_minmax(150px,0.9fr)_96px]'
            }`}
            onSubmit={submit}
        >
            <label className="grid gap-2 text-xs font-bold">
                정산번호
                <input
                    className="h-11 border border-line bg-surface px-3 text-sm text-ink"
                    inputMode="numeric"
                    pattern="\d*"
                    placeholder="정산번호"
                    value={requestId}
                    onChange={(event) => setRequestId(event.target.value.replace(/\D/g, ''))}
                />
            </label>
            {role === 'admin' && (
                <label className="grid gap-2 text-xs font-bold min-[701px]:ml-4">
                    판매자
                    <input
                        className="h-11 border border-line bg-surface px-3 text-sm text-ink"
                        placeholder="상점명을 입력하세요"
                        value={sellerKeyword}
                        onChange={(event) => setSellerKeyword(event.target.value)}
                    />
                </label>
            )}
            {role === 'seller' && (
                <SettlementRequestedDateFilter
                    requestedFrom={requestedFrom}
                    requestedTo={requestedTo}
                    onFromChange={setRequestedFrom}
                    onToChange={setRequestedTo}
                />
            )}
            <label className="grid gap-2 text-xs font-bold">
                처리 상태
                <select
                    className="h-11 border border-line bg-surface px-3 text-sm text-ink"
                    value={status}
                    onChange={(event) => setStatus(event.target.value)}
                >
                    <option value="">전체 상태</option>
                    <option value={role === 'admin' ? 'ACTION_REQUIRED' : 'PROCESSING'}>
                        {role === 'admin' ? '처리 필요' : '처리 중'}
                    </option>
                    {settlementStatuses.map((settlementStatus) => (
                        <option key={settlementStatus} value={settlementStatus}>
                            {settlementStatusLabel[settlementStatus]}
                        </option>
                    ))}
                </select>
            </label>
            {role === 'admin' && (
                <SettlementRequestedDateFilter
                    requestedFrom={requestedFrom}
                    requestedTo={requestedTo}
                    onFromChange={setRequestedFrom}
                    onToChange={setRequestedTo}
                />
            )}
            <button
                className="h-11 self-end bg-ink px-5 text-xs font-bold text-surface"
                type="submit"
            >
                검색
            </button>
        </form>
    )
}

function parseWorkType(
    role: 'seller' | 'admin',
    value: string | null,
): SettlementRequestWorkType | undefined {
    if (role === 'seller' && value === 'PROCESSING') return value
    if (role === 'admin' && value === 'ACTION_REQUIRED') return value
    return undefined
}

function SettlementRequestedDateFilter({
    requestedFrom,
    requestedTo,
    onFromChange,
    onToChange,
}: {
    requestedFrom: string
    requestedTo: string
    onFromChange: (value: string) => void
    onToChange: (value: string) => void
}) {
    return (
        <div
            className="grid min-w-0 gap-2"
            role="group"
            aria-label="신청일 전후"
        >
            <span className="text-xs font-bold">신청일 전후</span>
            <div className="grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-2">
                <input
                    aria-label="신청일 시작"
                    className="h-11 min-w-0 border border-line bg-surface px-2 text-sm text-ink"
                    max={requestedTo || undefined}
                    type="date"
                    value={requestedFrom}
                    onChange={(event) => onFromChange(event.target.value)}
                />
                <span className="text-xs text-muted" aria-hidden="true">~</span>
                <input
                    aria-label="신청일 종료"
                    className="h-11 min-w-0 border border-line bg-surface px-2 text-sm text-ink"
                    min={requestedFrom || undefined}
                    type="date"
                    value={requestedTo}
                    onChange={(event) => onToChange(event.target.value)}
                />
            </div>
        </div>
    )
}
