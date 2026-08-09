import { ArrowLeft, Check, LoaderCircle, Search, X } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
    getAdminProduct,
    getAdminProducts,
    updateAdminProductStatus,
} from '../api/admin'
import { ApiError } from '../api/client'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { StatusBadge, type StatusBadgeTone } from '../components/ui/StatusBadge'
import type { AdminProduct } from '../types/admin'
import type { ProductStatus } from '../types/product'
import { formatKoreanDateTime } from '../utils/dateTime'
import { formatPrice } from '../utils/product'

type ReviewStatus = Extract<ProductStatus, 'PENDING' | 'APPROVED' | 'REJECTED'>

const statusLabel: Record<ReviewStatus, string> = {
    PENDING: '승인 대기',
    APPROVED: '승인',
    REJECTED: '반려',
}

export function AdminProductReviewListPage() {
    const navigate = useNavigate()
    const [status, setStatus] = useState<ReviewStatus>('PENDING')
    const [keywordInput, setKeywordInput] = useState('')
    const [keyword, setKeyword] = useState('')
    const [page, setPage] = useState(1)
    const [products, setProducts] = useState<AdminProduct[]>([])
    const [totalPages, setTotalPages] = useState(0)
    const [totalElements, setTotalElements] = useState(0)
    const [isLoading, setIsLoading] = useState(true)
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        getAdminProducts({ status, keyword, page, signal: controller.signal })
            .then((response) => {
                setProducts(response.content)
                setTotalPages(response.totalPages)
                setTotalElements(response.totalElements)
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError
                    ? error.message
                    : '상품 검수 목록을 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [keyword, page, status])

    function search(event: FormEvent) {
        event.preventDefault()
        const nextKeyword = keywordInput.trim()
        if (keyword === nextKeyword && page === 1) return
        setIsLoading(true)
        setErrorMessage('')
        setPage(1)
        setKeyword(nextKeyword)
    }

    return (
        <section className="mx-auto max-w-350 px-4 py-10 min-[601px]:px-8 min-[601px]:py-14">
            <div className="mb-8">
                <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-accent">
                    PRODUCT REVIEW
                </p>
                <div className="flex flex-wrap items-end justify-between gap-4">
                    <div>
                        <h1 className="font-serif text-[clamp(36px,5vw,56px)] leading-none tracking-tighter">
                            상품 승인 관리
                        </h1>
                        <p className="mt-3 text-sm text-muted">
                            이미지와 상품 정보를 확인한 뒤 승인 또는 반려합니다.
                        </p>
                    </div>
                    <strong className="text-sm">총 {totalElements.toLocaleString()}건</strong>
                </div>
                <Link className="mt-5 inline-flex border border-line px-4 py-2 text-xs font-bold" to="/admin/product-change-requests">변경 심사 관리</Link>
            </div>

            <form
                className="mb-6 grid gap-3 border border-line bg-surface p-4 min-[701px]:grid-cols-[180px_1fr_auto]"
                onSubmit={search}
            >
                <label className="grid gap-1.5 text-xs font-bold">
                    심사 상태
                    <select
                        className="h-11 border border-line bg-paper px-3 text-sm font-normal text-ink"
                        value={status}
                        onChange={(event) => {
                            setIsLoading(true)
                            setErrorMessage('')
                            setStatus(event.target.value as ReviewStatus)
                            setPage(1)
                        }}
                    >
                        {Object.entries(statusLabel).map(([value, label]) => (
                            <option key={value} value={value}>{label}</option>
                        ))}
                    </select>
                </label>
                <label className="grid gap-1.5 text-xs font-bold">
                    상품·브랜드·판매자 검색
                    <input
                        className="h-11 border border-line bg-paper px-3 text-sm font-normal text-ink"
                        value={keywordInput}
                        onChange={(event) => setKeywordInput(event.target.value)}
                        placeholder="검색어를 입력하세요"
                    />
                </label>
                <button
                    className="mt-auto flex h-11 items-center justify-center gap-2 bg-ink px-6 text-xs font-bold text-paper"
                    type="submit"
                >
                    <Search className="size-4" />검색
                </button>
            </form>

            {errorMessage && <FeedbackMessage className="mb-5" tone="error">{errorMessage}</FeedbackMessage>}

            <div className="overflow-x-auto border-t-2 border-ink">
                <table className="w-full min-w-210 border-collapse text-left">
                    <thead className="bg-surface text-xs text-muted">
                        <tr>
                            <th className="px-4 py-3">상품</th>
                            <th className="px-4 py-3">판매자</th>
                            <th className="px-4 py-3">카테고리</th>
                            <th className="px-4 py-3">가격</th>
                            <th className="px-4 py-3">상태</th>
                            <th className="px-4 py-3">등록일</th>
                        </tr>
                    </thead>
                    <tbody>
                        {isLoading ? (
                            <tr><td className="h-40 text-center" colSpan={6}><LoaderCircle className="mx-auto size-6 animate-spin" /></td></tr>
                        ) : products.length === 0 ? (
                            <tr><td className="h-40 text-center text-sm text-muted" colSpan={6}>조건에 맞는 상품이 없습니다.</td></tr>
                        ) : products.map((product) => (
                            <tr
                                className="cursor-pointer border-t border-line transition-colors hover:bg-surface"
                                key={product.productId}
                                onClick={() => navigate(`/admin/products/${product.productId}`)}
                            >
                                <td className="px-4 py-3">
                                    <div className="flex items-center gap-3">
                                        <ProductThumbnail product={product} />
                                        <div>
                                            <strong className="line-clamp-1 text-sm">{product.name}</strong>
                                            <span className="mt-1 block text-xs text-muted">{product.brand || '브랜드 없음'}</span>
                                        </div>
                                    </div>
                                </td>
                                <td className="px-4 py-3 text-sm">{product.storeName || 'YMall'}</td>
                                <td className="px-4 py-3 text-sm">{product.categoryName}</td>
                                <td className="px-4 py-3 text-sm font-bold">{formatPrice(product.price)}</td>
                                <td className="px-4 py-3"><ProductReviewStatusBadge status={product.status as ReviewStatus} /></td>
                                <td className="px-4 py-3 text-xs text-muted">{formatKoreanDateTime(product.createdAt)}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {totalPages > 1 && (
                <div className="mt-6 flex items-center justify-center gap-2">
                    <button className="h-10 border border-line px-4 text-xs font-bold disabled:opacity-40" disabled={page === 1} type="button" onClick={() => { setIsLoading(true); setPage((current) => current - 1) }}>이전</button>
                    <span className="px-3 text-sm font-bold">{page} / {totalPages}</span>
                    <button className="h-10 border border-line px-4 text-xs font-bold disabled:opacity-40" disabled={page === totalPages} type="button" onClick={() => { setIsLoading(true); setPage((current) => current + 1) }}>다음</button>
                </div>
            )}
        </section>
    )
}

export function AdminProductReviewDetailPage() {
    const { productId } = useParams()
    const navigate = useNavigate()
    const numericProductId = Number(productId)
    const [product, setProduct] = useState<AdminProduct | null>(null)
    const [rejectionReason, setRejectionReason] = useState('')
    const [isLoading, setIsLoading] = useState(true)
    const [isProcessing, setIsProcessing] = useState(false)
    const [message, setMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        getAdminProduct(numericProductId, controller.signal)
            .then((response) => {
                setProduct(response)
                setRejectionReason(response.rejectionReason ?? '')
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError
                    ? error.message
                    : '상품 검수 정보를 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [numericProductId])

    async function review(status: 'APPROVED' | 'REJECTED') {
        if (status === 'REJECTED' && !rejectionReason.trim()) {
            setErrorMessage('반려 사유를 입력해 주세요.')
            return
        }
        setIsProcessing(true)
        setMessage('')
        setErrorMessage('')
        try {
            const updated = await updateAdminProductStatus(
                numericProductId,
                status,
                status === 'REJECTED' ? rejectionReason.trim() : undefined,
            )
            setProduct(updated)
            setMessage(status === 'APPROVED'
                ? '상품을 승인했습니다.'
                : '상품을 반려하고 사유를 판매자에게 전달했습니다.')
        } catch (error) {
            setErrorMessage(error instanceof ApiError
                ? error.message
                : '상품 심사 결과를 저장하지 못했습니다.')
        } finally {
            setIsProcessing(false)
        }
    }

    if (isLoading) {
        return <div className="grid min-h-100 place-content-center"><LoaderCircle className="size-6 animate-spin" /></div>
    }

    if (!product) {
        return (
            <section className="mx-auto max-w-300 px-4 py-12">
                <FeedbackMessage tone="error">{errorMessage || '상품을 찾을 수 없습니다.'}</FeedbackMessage>
            </section>
        )
    }

    const galleryImages = product.images.length > 0
        ? product.images.map((image) => image.imageUrl)
        : product.thumbnailUrl ? [product.thumbnailUrl] : []
    const canReview = product.status === 'PENDING'

    return (
        <section className="mx-auto max-w-330 px-4 py-10 min-[601px]:px-8 min-[601px]:py-14">
            <button className="mb-7 flex items-center gap-2 text-sm font-bold" type="button" onClick={() => navigate('/admin/products')}>
                <ArrowLeft className="size-4" />상품 승인 목록
            </button>
            <div className="mb-7 flex flex-wrap items-start justify-between gap-4">
                <div>
                    <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-accent">PRODUCT REVIEW</p>
                    <h1 className="font-serif text-[clamp(34px,5vw,52px)] leading-tight tracking-tighter">{product.name}</h1>
                </div>
                <ProductReviewStatusBadge status={product.status as ReviewStatus} />
            </div>

            {message && <FeedbackMessage className="mb-5" tone="success">{message}</FeedbackMessage>}
            {errorMessage && <FeedbackMessage className="mb-5" tone="error">{errorMessage}</FeedbackMessage>}

            <div className="grid gap-8 min-[901px]:grid-cols-[minmax(0,1.05fr)_minmax(340px,.95fr)]">
                <div className="grid gap-5">
                    <section className="border border-line bg-surface p-5">
                        <h2 className="mb-4 text-lg font-bold">상품 이미지</h2>
                        {galleryImages.length === 0 ? (
                            <div className="grid aspect-square max-h-150 place-content-center bg-paper text-sm text-muted">등록된 이미지가 없습니다.</div>
                        ) : (
                            <div className="grid grid-cols-2 gap-3">
                                {galleryImages.map((imageUrl, index) => (
                                    <img className="aspect-square w-full bg-paper object-contain" key={`${imageUrl}-${index}`} src={imageUrl} alt={`${product.name} 상품 이미지 ${index + 1}`} />
                                ))}
                            </div>
                        )}
                    </section>
                    {product.detailImages.length > 0 && (
                        <section className="border border-line bg-surface p-5">
                            <h2 className="mb-4 text-lg font-bold">상세 설명 이미지</h2>
                            <div className="grid gap-3">
                                {product.detailImages.map((image, index) => (
                                    <img className="w-full bg-paper object-contain" key={image.detailImageId ?? index} src={image.imageUrl} alt={`${product.name} 상세 이미지 ${index + 1}`} />
                                ))}
                            </div>
                        </section>
                    )}
                </div>

                <div className="grid content-start gap-5">
                    <section className="border-t-2 border-ink bg-surface p-5">
                        <h2 className="mb-5 text-lg font-bold">검수 정보</h2>
                        <dl className="grid grid-cols-[110px_1fr] gap-x-4 gap-y-4 text-sm">
                            <dt className="text-muted">판매자</dt><dd className="font-bold">{product.storeName || 'YMall'}</dd>
                            <dt className="text-muted">카테고리</dt><dd>{product.categoryName}</dd>
                            <dt className="text-muted">브랜드</dt><dd>{product.brand || '없음'}</dd>
                            <dt className="text-muted">판매가</dt><dd className="font-bold">{formatPrice(product.price)}</dd>
                            <dt className="text-muted">할인율</dt><dd>{product.discountPercentage}%</dd>
                            <dt className="text-muted">재고</dt><dd>{product.stock.toLocaleString()}개</dd>
                            <dt className="text-muted">등록일</dt><dd>{formatKoreanDateTime(product.createdAt)}</dd>
                        </dl>
                    </section>
                    <section className="border border-line bg-surface p-5">
                        <h2 className="mb-3 text-lg font-bold">상품 설명</h2>
                        <p className="whitespace-pre-wrap text-sm leading-7 text-muted">{product.description || '등록된 설명이 없습니다.'}</p>
                    </section>
                    {product.rejectionReason && !canReview && (
                        <FeedbackMessage tone="error">반려 사유: {product.rejectionReason}</FeedbackMessage>
                    )}
                    {canReview && (
                        <section className="border border-line bg-surface p-5">
                            <div className="grid gap-2">
                                <label className="text-sm font-bold" htmlFor="product-rejection-reason">
                                    반려 사유
                                </label>
                                <textarea
                                    className="min-h-28 resize-y border border-line bg-paper p-3 font-normal text-ink"
                                    id="product-rejection-reason"
                                    value={rejectionReason}
                                    maxLength={500}
                                    placeholder="반려할 경우 판매자가 수정할 수 있도록 구체적인 사유를 입력해 주세요."
                                    onChange={(event) => setRejectionReason(event.target.value)}
                                />
                                <span className="text-right text-xs font-normal text-muted">{rejectionReason.length} / 500</span>
                            </div>
                            <div className="mt-4 grid grid-cols-2 gap-3">
                                <button className="flex h-11 items-center justify-center gap-2 bg-ink text-xs font-bold text-paper disabled:opacity-50" disabled={isProcessing} type="button" onClick={() => void review('APPROVED')}>
                                    <Check className="size-4" />승인
                                </button>
                                <button className="flex h-11 items-center justify-center gap-2 border border-danger text-xs font-bold text-danger disabled:opacity-50" disabled={isProcessing} type="button" onClick={() => void review('REJECTED')}>
                                    <X className="size-4" />반려
                                </button>
                            </div>
                        </section>
                    )}
                </div>
            </div>
        </section>
    )
}

function ProductThumbnail({ product }: { product: AdminProduct }) {
    const imageUrl = product.thumbnailUrl || product.images[0]?.imageUrl
    return imageUrl
        ? <img className="size-14 shrink-0 rounded-md bg-paper object-cover" src={imageUrl} alt="" />
        : <span className="grid size-14 shrink-0 place-content-center rounded-md bg-paper text-[10px] text-muted">NO IMAGE</span>
}

function ProductReviewStatusBadge({ status }: { status: ReviewStatus }) {
    const tone: StatusBadgeTone = status === 'APPROVED'
        ? 'success'
        : status === 'REJECTED'
            ? 'danger'
            : 'warning'
    return <StatusBadge tone={tone}>{statusLabel[status]}</StatusBadge>
}
