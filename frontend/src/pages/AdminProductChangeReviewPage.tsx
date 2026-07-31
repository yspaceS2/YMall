import { ArrowLeft, Check, LoaderCircle, X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'

import {
    getAdminProductChangeRequest,
    getAdminProductChangeRequests,
    reviewAdminProductChangeRequest,
} from '../api/admin'
import { ApiError } from '../api/client'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import type {
    ProductChangeRequest,
    ProductChangeRequestStatus,
    ProductSnapshot,
} from '../types/admin'
import { formatKoreanDateTime } from '../utils/dateTime'
import { formatPrice } from '../utils/product'

export function AdminProductChangeReviewListPage() {
    const navigate = useNavigate()
    const [status, setStatus] = useState<ProductChangeRequestStatus>('PENDING')
    const [page, setPage] = useState(1)
    const requestKey = `${status}:${page}`
    const [result, setResult] = useState<{
        key: string
        requests: ProductChangeRequest[]
        totalPages: number
        error: string
    } | null>(null)
    const isLoading = result?.key !== requestKey
    const requests = isLoading ? [] : result.requests
    const totalPages = isLoading ? 0 : result.totalPages
    const error = isLoading ? '' : result.error

    useEffect(() => {
        const controller = new AbortController()
        getAdminProductChangeRequests(status, page, controller.signal)
            .then((response) => {
                setResult({
                    key: requestKey,
                    requests: response.content,
                    totalPages: response.totalPages,
                    error: '',
                })
            })
            .catch((requestError: unknown) => {
                if (requestError instanceof Error && requestError.name === 'AbortError') return
                setResult({
                    key: requestKey,
                    requests: [],
                    totalPages: 0,
                    error: requestError instanceof ApiError
                        ? requestError.message
                        : '변경 심사 목록을 불러오지 못했습니다.',
                })
            })
        return () => controller.abort()
    }, [page, requestKey, status])

    return (
        <section className="mx-auto max-w-350 px-4 py-10 min-[601px]:px-8">
            <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
                <div>
                    <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">CHANGE REVIEW</p>
                    <h1 className="font-serif text-5xl tracking-tight">상품 변경 심사</h1>
                    <p className="mt-3 text-sm text-muted">현재 판매 정보는 유지한 채 판매자가 제출한 변경안만 검토합니다.</p>
                </div>
                <Link className="border border-line px-4 py-2 text-xs font-bold" to="/admin/products">최초 승인 관리</Link>
            </div>
            {error && <FeedbackMessage className="mb-5" tone="error">{error}</FeedbackMessage>}
            <div className="mb-5 flex flex-wrap gap-2" aria-label="변경 심사 상태">
                {([
                    ['PENDING', '심사 대기'],
                    ['APPROVED', '승인'],
                    ['REJECTED', '반려'],
                ] as const).map(([value, label]) => (
                    <button
                        className={`h-10 border px-4 text-xs font-bold ${status === value ? 'border-ink bg-ink text-paper' : 'border-line bg-paper text-ink'}`}
                        key={value}
                        onClick={() => {
                            setStatus(value)
                            setPage(1)
                        }}
                        type="button"
                    >
                        {label}
                    </button>
                ))}
            </div>
            <div className="overflow-x-auto border-t-2 border-ink">
                <table className="w-full min-w-190 border-collapse text-left">
                    <thead className="bg-surface text-xs text-muted">
                        <tr>
                            <th className="px-4 py-3">상품</th>
                            <th className="px-4 py-3">판매자</th>
                            <th className="px-4 py-3">변경 항목</th>
                            <th className="px-4 py-3">요청일</th>
                        </tr>
                    </thead>
                    <tbody>
                        {isLoading ? (
                            <tr><td className="h-40 text-center" colSpan={4}><LoaderCircle className="mx-auto size-6 animate-spin" /></td></tr>
                        ) : requests.length === 0 ? (
                            <tr><td className="h-40 text-center text-sm text-muted" colSpan={4}>해당 상태의 변경 심사가 없습니다.</td></tr>
                        ) : requests.map((request) => (
                            <tr
                                className="cursor-pointer border-t border-line hover:bg-surface"
                                key={request.productChangeRequestId}
                                onClick={() => navigate(`/admin/product-change-requests/${request.productChangeRequestId}`)}
                            >
                                <td className="px-4 py-4 font-bold">{request.current.name}</td>
                                <td className="px-4 py-4 text-sm">{request.storeName ?? 'YMall'}</td>
                                <td className="px-4 py-4 text-sm">{changedLabels(request.current, request.proposed).join(', ')}</td>
                                <td className="px-4 py-4 text-xs text-muted">{formatKoreanDateTime(request.createdAt)}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            {totalPages > 1 && (
                <nav className="mt-6 flex items-center justify-center gap-3" aria-label="변경 심사 페이지">
                    <button className="border border-line px-4 py-2 text-xs font-bold disabled:opacity-40" disabled={page <= 1} onClick={() => setPage((current) => current - 1)} type="button">이전</button>
                    <span className="text-xs text-muted">{page} / {totalPages}</span>
                    <button className="border border-line px-4 py-2 text-xs font-bold disabled:opacity-40" disabled={page >= totalPages} onClick={() => setPage((current) => current + 1)} type="button">다음</button>
                </nav>
            )}
        </section>
    )
}

export function AdminProductChangeReviewDetailPage() {
    const { requestId } = useParams()
    const numericRequestId = Number(requestId)
    const [request, setRequest] = useState<ProductChangeRequest | null>(null)
    const [reason, setReason] = useState('')
    const [isLoading, setIsLoading] = useState(true)
    const [isProcessing, setIsProcessing] = useState(false)
    const [error, setError] = useState('')
    const [message, setMessage] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        getAdminProductChangeRequest(numericRequestId, controller.signal)
            .then(setRequest)
            .catch((requestError: unknown) => {
                if (requestError instanceof Error && requestError.name === 'AbortError') return
                setError(requestError instanceof ApiError
                    ? requestError.message
                    : '변경 심사 정보를 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [numericRequestId])

    const rows = useMemo(
        () => request ? comparisonRows(request.current, request.proposed) : [],
        [request],
    )

    async function review(status: 'APPROVED' | 'REJECTED') {
        if (status === 'REJECTED' && !reason.trim()) {
            setError('반려 사유를 입력해 주세요.')
            return
        }
        setIsProcessing(true)
        setError('')
        try {
            const updated = await reviewAdminProductChangeRequest(
                numericRequestId,
                status,
                status === 'REJECTED' ? reason.trim() : undefined,
            )
            setRequest(updated)
            setMessage(status === 'APPROVED'
                ? '변경안을 승인해 판매 상품에 반영했습니다.'
                : '변경안을 반려했습니다.')
        } catch (requestError) {
            setError(requestError instanceof ApiError
                ? requestError.message
                : '변경 심사를 처리하지 못했습니다.')
        } finally {
            setIsProcessing(false)
        }
    }

    if (isLoading) return <div className="grid min-h-100 place-content-center"><LoaderCircle className="size-6 animate-spin" /></div>
    if (!request) return <section className="mx-auto max-w-300 p-8"><FeedbackMessage tone="error">{error}</FeedbackMessage></section>

    return (
        <section className="mx-auto max-w-330 px-4 py-10 min-[601px]:px-8">
            <Link className="mb-7 inline-flex items-center gap-2 text-sm font-bold" to="/admin/product-change-requests">
                <ArrowLeft className="size-4" /> 변경 심사 목록
            </Link>
            <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">BEFORE / AFTER</p>
            <h1 className="font-serif text-5xl tracking-tight">{request.current.name}</h1>
            <p className="mt-3 text-sm text-muted">노란색 행이 실제로 변경된 항목입니다.</p>
            {message && <FeedbackMessage className="mt-6" tone="success">{message}</FeedbackMessage>}
            {error && <FeedbackMessage className="mt-6" tone="error">{error}</FeedbackMessage>}

            <div className="mt-8 overflow-x-auto border-t-2 border-ink">
                <table className="w-full min-w-190 border-collapse text-left text-sm">
                    <thead className="bg-surface"><tr><th className="w-36 px-4 py-3">항목</th><th className="px-4 py-3">현재 판매 정보</th><th className="px-4 py-3">변경 요청 정보</th></tr></thead>
                    <tbody>{rows.map((row) => (
                        <tr className={`border-t border-line ${row.changed ? 'bg-[#fff8cf] dark:bg-[#4a421f]' : ''}`} key={row.label}>
                            <th className="px-4 py-4">{row.label}</th>
                            <td className="whitespace-pre-wrap px-4 py-4 text-muted">{row.before}</td>
                            <td className="whitespace-pre-wrap px-4 py-4 font-medium">{row.after}</td>
                        </tr>
                    ))}</tbody>
                </table>
            </div>

            <div className="mt-8 grid gap-5 lg:grid-cols-2">
                <ProductImageComparison title="현재 판매 이미지" snapshot={request.current} />
                <ProductImageComparison title="변경 요청 이미지" snapshot={request.proposed} />
            </div>

            {request.status === 'PENDING' && (
                <section className="mt-6 border border-line bg-surface p-5">
                    <label className="grid gap-2 text-sm font-bold">
                        반려 사유
                        <textarea className="min-h-24 border border-line bg-paper p-3 text-ink" value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} />
                    </label>
                    <div className="mt-4 flex gap-3">
                        <button className="flex h-11 items-center gap-2 bg-ink px-6 text-xs font-bold text-paper disabled:opacity-50" disabled={isProcessing} type="button" onClick={() => void review('APPROVED')}><Check className="size-4" />승인</button>
                        <button className="flex h-11 items-center gap-2 border border-[#a22e24] px-6 text-xs font-bold text-[#a22e24] disabled:opacity-50" disabled={isProcessing} type="button" onClick={() => void review('REJECTED')}><X className="size-4" />반려</button>
                    </div>
                </section>
            )}
        </section>
    )
}

function ProductImageComparison({
    title,
    snapshot,
}: {
    title: string
    snapshot: ProductSnapshot
}) {
    const productImages = snapshot.images.length > 0
        ? snapshot.images.map((image) => image.imageUrl)
        : snapshot.thumbnailUrl ? [snapshot.thumbnailUrl] : []
    const detailImages = snapshot.detailImages.map((image) => image.imageUrl)

    return (
        <section className="border border-line bg-surface p-5">
            <h2 className="text-sm font-extrabold">{title}</h2>
            <p className="mt-1 text-xs text-muted">상품 이미지와 상세 설명 이미지를 직접 확인해 주세요.</p>
            <div className="mt-4 grid grid-cols-3 gap-2">
                {productImages.length === 0 ? (
                    <p className="col-span-3 py-8 text-center text-xs text-muted">등록된 상품 이미지가 없습니다.</p>
                ) : productImages.map((imageUrl, index) => (
                    <img
                        alt={`${title} 상품 이미지 ${index + 1}`}
                        className="aspect-square w-full border border-line bg-paper object-cover"
                        key={`${imageUrl}-${index}`}
                        loading="lazy"
                        referrerPolicy="no-referrer"
                        src={imageUrl}
                    />
                ))}
            </div>
            <div className="mt-5 grid gap-3">
                <h3 className="text-xs font-bold">상세 설명 이미지</h3>
                {detailImages.length === 0 ? (
                    <p className="py-6 text-center text-xs text-muted">등록된 상세 이미지가 없습니다.</p>
                ) : detailImages.map((imageUrl, index) => (
                    <img
                        alt={`${title} 상세 이미지 ${index + 1}`}
                        className="w-full border border-line bg-paper object-contain"
                        key={`${imageUrl}-${index}`}
                        loading="lazy"
                        referrerPolicy="no-referrer"
                        src={imageUrl}
                    />
                ))}
            </div>
        </section>
    )
}

function changedLabels(current: ProductSnapshot, proposed: ProductSnapshot) {
    return comparisonRows(current, proposed).filter((row) => row.changed).map((row) => row.label)
}

function comparisonRows(current: ProductSnapshot, proposed: ProductSnapshot) {
    const values = [
        ['카테고리', current.categoryName, proposed.categoryName],
        ['상품명', current.name, proposed.name],
        ['브랜드', current.brand ?? '-', proposed.brand ?? '-'],
        ['가격', formatPrice(current.price), formatPrice(proposed.price)],
        ['할인율', `${current.discountPercentage}%`, `${proposed.discountPercentage}%`],
        ['할인 기간', period(current), period(proposed)],
        ['재고', `${current.stock}개`, `${proposed.stock}개`],
        ['배송비', shipping(current), shipping(proposed)],
        ['예상 배송', `${current.estimatedDeliveryDays}일`, `${proposed.estimatedDeliveryDays}일`],
        ['설명', current.description ?? '-', proposed.description ?? '-'],
        ['대표 이미지', current.thumbnailUrl ?? '-', proposed.thumbnailUrl ?? '-'],
        ['상품 이미지', `${current.images.length}장`, `${proposed.images.length}장`],
        ['상세 이미지', `${current.detailImages.length}장`, `${proposed.detailImages.length}장`],
    ]
    return values.map(([label, before, after]) => ({
        label,
        before,
        after,
        changed: before !== after,
    }))
}

function period(snapshot: ProductSnapshot) {
    if (snapshot.discountPercentage <= 0) return '할인 없음'
    return `${snapshot.discountStartDate ?? '-'} ~ ${snapshot.discountEndDate ?? '-'}`
}

function shipping(snapshot: ProductSnapshot) {
    return snapshot.freeShipping ? '무료배송' : formatPrice(snapshot.shippingFee)
}
