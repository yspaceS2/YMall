import { LoaderCircle, PackagePlus, Pencil, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import {
    deleteSellerProduct,
    getSellerProducts,
} from '../api/seller'
import { getCategories } from '../api/products'
import {
    ManagementEmpty,
    ManagementListSearch,
    ManagementPageHeader,
    ManagementPagination,
    managementPageClassName,
} from '../components/management/ManagementListUi'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import type { Category, ProductSummary } from '../types/product'
import type { SellerProductStockCondition } from '../types/seller'
import { formatPrice, resolveImageUrl } from '../utils/product'
import { getCategoryChildren } from '../utils/productCategory'

const PAGE_SIZE = 20

export function SellerProductListPage() {
    const navigate = useNavigate()
    const [searchParams, setSearchParams] = useSearchParams()
    const [products, setProducts] = useState<ProductSummary[]>([])
    const [categories, setCategories] = useState<Category[]>([])
    const [pagination, setPagination] = useState({ page: 1, totalPages: 0, totalElements: 0 })
    const [isLoading, setIsLoading] = useState(true)
    const [errorMessage, setErrorMessage] = useState('')
    const [productToDelete, setProductToDelete] = useState<ProductSummary | null>(null)
    const [isDeleting, setIsDeleting] = useState(false)
    const page = positiveNumber(searchParams.get('page'), 1)
    const keyword = searchParams.get('keyword') ?? ''
    const rootCategoryId = positiveNumber(searchParams.get('rootCategoryId'), 0) || undefined
    const middleCategoryId = positiveNumber(searchParams.get('middleCategoryId'), 0) || undefined
    const leafCategoryId = positiveNumber(searchParams.get('categoryId'), 0) || undefined
    const selectedCategoryId = leafCategoryId ?? middleCategoryId ?? rootCategoryId
    const stockCondition = parseProductStockCondition(searchParams.get('stockCondition'))
    const stockQuantity = nonNegativeNumber(searchParams.get('stockQuantity'))
    const rootCategories = getCategoryChildren(categories, null)
    const middleCategories = rootCategoryId
        ? getCategoryChildren(categories, rootCategoryId)
        : []
    const leafCategories = middleCategoryId
        ? getCategoryChildren(categories, middleCategoryId)
        : []

    useEffect(() => {
        const controller = new AbortController()
        getCategories(controller.signal)
            .then(setCategories)
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(
                    error instanceof ApiError
                        ? error.message
                        : '카테고리를 불러오지 못했습니다.',
                )
            })
        return () => controller.abort()
    }, [])

    useEffect(() => {
        const controller = new AbortController()
        getSellerProducts({
            page,
            size: PAGE_SIZE,
            keyword,
            categoryId: selectedCategoryId,
            stockCondition,
            stockQuantity,
            signal: controller.signal,
        })
            .then((response) => {
                setProducts(response.content)
                setPagination({
                    page: response.page,
                    totalPages: response.totalPages,
                    totalElements: response.totalElements,
                })
                setErrorMessage('')
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError ? error.message : '상품 목록을 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [keyword, page, selectedCategoryId, stockCondition, stockQuantity])

    function updateCategoryFilter(
        name: 'rootCategoryId' | 'middleCategoryId' | 'categoryId',
        value: string,
    ) {
        const next = new URLSearchParams(searchParams)
        if (value) next.set(name, value)
        else next.delete(name)
        if (name === 'rootCategoryId') {
            next.delete('middleCategoryId')
            next.delete('categoryId')
        } else if (name === 'middleCategoryId') {
            next.delete('categoryId')
        }
        next.set('page', '1')
        setIsLoading(true)
        setSearchParams(next)
    }

    function updateStockFilter(name: 'stockCondition' | 'stockQuantity', value: string) {
        const next = new URLSearchParams(searchParams)
        if (value) next.set(name, value)
        else next.delete(name)
        next.set('page', '1')
        setIsLoading(true)
        setSearchParams(next)
    }

    async function removeProduct() {
        if (!productToDelete) return
        setIsDeleting(true)
        try {
            await deleteSellerProduct(productToDelete.productId)
            setProducts((current) => current.filter(
                (product) => product.productId !== productToDelete.productId,
            ))
            setPagination((current) => ({
                ...current,
                totalElements: Math.max(0, current.totalElements - 1),
            }))
            setProductToDelete(null)
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '상품을 삭제하지 못했습니다.')
        } finally {
            setIsDeleting(false)
        }
    }

    return (
        <section className={managementPageClassName}>
            <ManagementPageHeader
                eyebrow="SELLER PRODUCTS"
                title="상품 관리"
                description={`등록 상품 ${pagination.totalElements.toLocaleString()}개`}
                action={(
                    <Link
                        className="flex h-11 items-center gap-2 bg-ink px-5 text-xs font-bold text-white"
                        to="/seller/products/new"
                    >
                        <PackagePlus className="size-4" />
                        상품 등록
                    </Link>
                )}
            />
            <div className="mb-5 grid gap-3 md:grid-cols-2 xl:grid-cols-[1fr_1fr_1fr_1.2fr]">
                <label className="grid gap-1.5 text-xs font-bold">
                    대분류
                    <select
                        className="h-11 border border-line bg-surface px-3 text-sm font-normal text-ink outline-0 focus:border-ink"
                        value={rootCategoryId ?? ''}
                        onChange={(event) => updateCategoryFilter(
                            'rootCategoryId',
                            event.target.value,
                        )}
                    >
                        <option value="">전체 대분류</option>
                        {rootCategories.map((category) => (
                            <option key={category.categoryId} value={category.categoryId}>
                                {category.name}
                            </option>
                        ))}
                    </select>
                </label>
                <label className="grid gap-1.5 text-xs font-bold">
                    중분류
                    <select
                        className="h-11 border border-line bg-surface px-3 text-sm font-normal text-ink outline-0 focus:border-ink"
                        disabled={!rootCategoryId}
                        value={middleCategoryId ?? ''}
                        onChange={(event) => updateCategoryFilter(
                            'middleCategoryId',
                            event.target.value,
                        )}
                    >
                        <option value="">전체 중분류</option>
                        {middleCategories.map((category) => (
                            <option key={category.categoryId} value={category.categoryId}>
                                {category.name}
                            </option>
                        ))}
                    </select>
                </label>
                <label className="grid gap-1.5 text-xs font-bold">
                    소분류
                    <select
                        className="h-11 border border-line bg-surface px-3 text-sm font-normal text-ink outline-0 focus:border-ink"
                        disabled={!middleCategoryId}
                        value={leafCategoryId ?? ''}
                        onChange={(event) => updateCategoryFilter(
                            'categoryId',
                            event.target.value,
                        )}
                    >
                        <option value="">전체 소분류</option>
                        {leafCategories.map((category) => (
                            <option key={category.categoryId} value={category.categoryId}>
                                {category.name}
                            </option>
                        ))}
                    </select>
                </label>
                <label className="grid gap-1.5 text-xs font-bold">
                    재고 수량
                    <span className="flex">
                        <input
                            className="h-11 min-w-0 flex-1 border border-r-0 border-line bg-surface px-3 text-sm font-normal text-ink outline-0 focus:border-ink"
                            min="0"
                            placeholder="수량 입력"
                            type="number"
                            value={stockQuantity ?? ''}
                            onChange={(event) => updateStockFilter(
                                'stockQuantity',
                                event.target.value,
                            )}
                        />
                        <select
                            aria-label="재고 수량 비교 조건"
                            className="h-11 border border-line bg-surface px-3 text-sm font-normal text-ink outline-0 focus:border-ink"
                            value={stockCondition}
                            onChange={(event) => updateStockFilter(
                                'stockCondition',
                                event.target.value,
                            )}
                        >
                            <option value="GTE">개 이상</option>
                            <option value="LTE">개 이하</option>
                        </select>
                    </span>
                </label>
            </div>
            <ManagementListSearch placeholder="상품명 또는 브랜드를 검색하세요" />
            {errorMessage && <FeedbackMessage className="mb-5" tone="error">{errorMessage}</FeedbackMessage>}
            {isLoading ? <Loading /> : products.length === 0 ? (
                <ManagementEmpty>조건에 맞는 상품이 없습니다.</ManagementEmpty>
            ) : (
                <div className="overflow-x-auto border-t-2 border-ink">
                    <table className="w-full min-w-190 text-left text-sm">
                        <thead className="border-b border-line bg-surface text-xs">
                            <tr>
                                <th className="p-4">상품</th>
                                <th className="p-4">카테고리</th>
                                <th className="p-4">가격</th>
                                <th className="p-4">재고</th>
                                <th className="p-4">상태</th>
                                <th className="p-4 text-right">관리</th>
                            </tr>
                        </thead>
                        <tbody>
                            {products.map((product) => (
                                <tr
                                    className="cursor-pointer border-b border-line bg-paper transition-colors hover:bg-surface focus-visible:bg-surface focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-ink"
                                    key={product.productId}
                                    onClick={() => navigate(`/seller/products/${product.productId}`)}
                                    onKeyDown={(event) => {
                                        if (event.target !== event.currentTarget) return
                                        if (event.key === 'Enter' || event.key === ' ') {
                                            event.preventDefault()
                                            navigate(`/seller/products/${product.productId}`)
                                        }
                                    }}
                                    role="link"
                                    tabIndex={0}
                                >
                                    <td className="p-4">
                                        <div className="flex min-w-60 items-center gap-3">
                                            <div className="size-14 shrink-0 overflow-hidden border border-line bg-surface">
                                                {product.thumbnailUrl ? (
                                                    <img
                                                        alt=""
                                                        className="size-full object-cover"
                                                        loading="lazy"
                                                        src={resolveImageUrl(product.thumbnailUrl)}
                                                    />
                                                ) : (
                                                    <div className="grid size-full place-items-center text-[9px] font-bold tracking-[.12em] text-muted">
                                                        YMALL
                                                    </div>
                                                )}
                                            </div>
                                            <div className="min-w-0">
                                                <strong className="block truncate">{product.name}</strong>
                                                <p className="mt-1 truncate text-xs text-muted">{product.brand || '브랜드 미입력'}</p>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="p-4">{product.categoryName}</td>
                                    <td className="p-4">{formatPrice(product.price)}</td>
                                    <td className="p-4">{product.stock.toLocaleString()}</td>
                                    <td className="p-4">{product.status}</td>
                                    <td className="p-4">
                                        <div className="flex justify-end gap-1">
                                            <Link
                                                className="grid size-9 place-items-center"
                                                to={`/seller/products/${product.productId}`}
                                                aria-label={`${product.name} 수정`}
                                                onClick={(event) => event.stopPropagation()}
                                            >
                                                <Pencil className="size-4" />
                                            </Link>
                                            <button
                                                className="grid size-9 place-items-center text-[#a22e24] dark:text-[#ffb7ae]"
                                                type="button"
                                                onClick={(event) => {
                                                    event.stopPropagation()
                                                    setProductToDelete(product)
                                                }}
                                                aria-label={`${product.name} 삭제`}
                                            >
                                                <Trash2 className="size-4" />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            <ManagementPagination page={pagination.page} totalPages={pagination.totalPages} />
            <ConfirmDialog
                open={productToDelete !== null}
                title="상품을 삭제할까요?"
                description={productToDelete ? `'${productToDelete.name}' 상품을 삭제합니다.` : ''}
                confirmLabel="상품 삭제"
                isPending={isDeleting}
                onCancel={() => setProductToDelete(null)}
                onConfirm={() => void removeProduct()}
            />
        </section>
    )
}

function positiveNumber(value: string | null, fallback: number) {
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
}

function Loading() {
    return (
        <div className="grid min-h-72 place-items-center">
            <LoaderCircle className="size-6 animate-spin" aria-label="불러오는 중" />
        </div>
    )
}

function parseProductStockCondition(
    value: string | null,
): SellerProductStockCondition {
    return value === 'LTE' ? 'LTE' : 'GTE'
}

function nonNegativeNumber(value: string | null) {
    if (value == null || value.trim() === '') return undefined
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed >= 0 ? parsed : undefined
}
