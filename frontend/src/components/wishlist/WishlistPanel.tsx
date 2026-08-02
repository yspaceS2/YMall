import { Heart, LoaderCircle, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

import { ApiError } from '../../api/client'
import { getWishlist, removeWishlistProduct } from '../../api/wishlist'
import type { WishlistProduct } from '../../types/wishlist'
import { formatKoreanDate } from '../../utils/dateTime'
import { formatPrice, getDiscountedPrice, resolveImageUrl } from '../../utils/product'
import { FeedbackMessage } from '../ui/FeedbackMessage'

const PAGE_SIZE = 8

function availability(product: WishlistProduct) {
    if (product.stock === 0 || product.status === 'SOLD_OUT') {
        return { label: '품절', className: 'text-danger' }
    }
    if (product.status !== 'APPROVED') {
        return { label: '판매 중지', className: 'text-muted' }
    }
    return { label: '판매 중', className: 'text-success' }
}

export function WishlistPanel() {
    const [products, setProducts] = useState<WishlistProduct[]>([])
    const [page, setPage] = useState(1)
    const [reloadKey, setReloadKey] = useState(0)
    const [hasNext, setHasNext] = useState(false)
    const [isLoading, setIsLoading] = useState(true)
    const [removingId, setRemovingId] = useState<number | null>(null)
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        getWishlist(page, PAGE_SIZE, controller.signal)
            .then((response) => {
                setProducts((current) => page === 1
                    ? response.content
                    : [...current, ...response.content])
                setHasNext(response.hasNext)
                setErrorMessage('')
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError
                    ? error.message
                    : '찜 목록을 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [page, reloadKey])

    async function remove(productId: number) {
        setRemovingId(productId)
        setErrorMessage('')
        try {
            await removeWishlistProduct(productId)
            setProducts([])
            setPage(1)
            setHasNext(false)
            setIsLoading(true)
            setReloadKey((current) => current + 1)
        } catch (error) {
            setErrorMessage(error instanceof ApiError
                ? error.message
                : '찜 상품을 삭제하지 못했습니다.')
        } finally {
            setRemovingId(null)
        }
    }

    return (
        <section
            className="mt-8 scroll-mt-24 border border-line bg-surface p-6 min-[601px]:p-8"
            id="wishlist"
        >
            <p className="text-[11px] font-extrabold tracking-[.16em] text-muted">WISHLIST</p>
            <h2 className="mt-2 flex items-center gap-2 font-serif text-3xl">
                <Heart className="size-6" aria-hidden="true" />
                찜한 상품
            </h2>
            <p className="mt-3 text-sm text-muted">
                관심 상품의 판매 상태와 가격을 한곳에서 확인할 수 있습니다.
            </p>

            {errorMessage && (
                <FeedbackMessage className="mt-5" tone="error">
                    {errorMessage}
                </FeedbackMessage>
            )}

            {isLoading && products.length === 0 ? (
                <div className="grid min-h-36 place-content-center" aria-label="찜 목록 로딩 중">
                    <LoaderCircle className="size-5 animate-spin" />
                </div>
            ) : products.length === 0 ? (
                <div className="mt-6 border border-dashed border-line p-8 text-center">
                    <p className="text-sm font-bold">아직 찜한 상품이 없습니다.</p>
                    <Link className="mt-3 inline-block text-xs font-bold underline" to="/">
                        상품 둘러보기
                    </Link>
                </div>
            ) : (
                <div className="mt-6 grid gap-4">
                    {products.map((product) => {
                        const state = availability(product)
                        const content = (
                            <>
                                <div className="size-20 shrink-0 overflow-hidden bg-paper">
                                    {product.thumbnailUrl ? (
                                        <img
                                            className="size-full object-cover"
                                            src={resolveImageUrl(product.thumbnailUrl)}
                                            alt=""
                                        />
                                    ) : (
                                        <div className="grid size-full place-items-center text-[10px] font-bold tracking-widest text-muted">
                                            YMALL
                                        </div>
                                    )}
                                </div>
                                <div className="min-w-0 flex-1">
                                    <p className="text-[10px] font-bold tracking-wider text-muted">
                                        {product.brand}
                                    </p>
                                    <strong className="mt-1 block truncate text-sm">
                                        {product.name}
                                    </strong>
                                    <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs">
                                        <b>{formatPrice(getDiscountedPrice(
                                            product.price,
                                            product.discountPercentage,
                                        ))}</b>
                                        <span className={state.className}>{state.label}</span>
                                        <span className="text-muted">
                                            {formatKoreanDate(product.wishedAt)} 찜
                                        </span>
                                    </div>
                                </div>
                            </>
                        )

                        return (
                            <article
                                className="flex items-center gap-4 border border-line p-3"
                                key={product.productId}
                            >
                                {product.status === 'APPROVED' ? (
                                    <Link
                                        className="flex min-w-0 flex-1 items-center gap-4"
                                        to={`/products/${product.productId}`}
                                    >
                                        {content}
                                    </Link>
                                ) : (
                                    <div className="flex min-w-0 flex-1 items-center gap-4">
                                        {content}
                                    </div>
                                )}
                                <button
                                    className="grid size-10 shrink-0 place-items-center border border-line text-muted hover:border-ink hover:text-ink disabled:opacity-50"
                                    type="button"
                                    aria-label={`${product.name} 찜 해제`}
                                    disabled={removingId === product.productId}
                                    onClick={() => void remove(product.productId)}
                                >
                                    {removingId === product.productId
                                        ? <LoaderCircle className="size-4 animate-spin" />
                                        : <Trash2 className="size-4" />}
                                </button>
                            </article>
                        )
                    })}
                </div>
            )}

            {hasNext && (
                <button
                    className="mx-auto mt-6 block h-10 border border-ink px-6 text-xs font-bold disabled:opacity-50"
                    type="button"
                    disabled={isLoading}
                    onClick={() => {
                        setIsLoading(true)
                        setPage((current) => current + 1)
                    }}
                >
                    {isLoading ? '불러오는 중...' : '찜 상품 더 보기'}
                </button>
            )}
        </section>
    )
}
