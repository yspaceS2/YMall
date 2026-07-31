import { ChevronLeft, Minus, Plus, ShieldCheck, Star, Truck } from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { addCartItem } from '../api/cart'
import { ApiError } from '../api/client'
import { getProduct } from '../api/products'
import { getProductReviews, getProductReviewSummary } from '../api/reviews'
import { useAuth } from '../auth/useAuth'
import { ReviewSummaryPanel } from '../components/review/ReviewSummaryPanel'
import { ProductQuestionSection } from '../components/productquestion/ProductQuestionSection'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { PageState } from '../components/ui/PageState'
import { ProductWishlistButton } from '../components/wishlist/ProductWishlistButton'
import { useToast } from '../toast/useToast'
import type { ProductDetail } from '../types/product'
import type { Review, ReviewSummary } from '../types/review'
import { formatKoreanDate } from '../utils/dateTime'
import { formatPrice, getDiscountedPrice, resolveImageUrl } from '../utils/product'

type ProductDetailTab = 'information' | 'reviews' | 'qna'

export function ProductDetailPage() {
    const { productId } = useParams()
    const { isAuthenticated } = useAuth()
    const location = useLocation()
    const navigate = useNavigate()
    const { showToast } = useToast()
    const id = Number(productId)
    const invalidProductId = !Number.isInteger(id)
    const [product, setProduct] = useState<ProductDetail | null>(null)
    const [error, setError] = useState('')
    const [quantity, setQuantity] = useState(1)
    const [selectedImage, setSelectedImage] = useState('')
    const [isAddingToCart, setIsAddingToCart] = useState(false)
    const [reviews, setReviews] = useState<Review[]>([])
    const [reviewCount, setReviewCount] = useState(0)
    const [reviewNextPage, setReviewNextPage] = useState(2)
    const [hasMoreReviews, setHasMoreReviews] = useState(false)
    const [isLoadingReviews, setIsLoadingReviews] = useState(false)
    const [reviewError, setReviewError] = useState('')
    const [reviewSummaryState, setReviewSummaryState] = useState<{
        productId: number | null
        summary: ReviewSummary | null
        isLoading: boolean
        error: string
    }>({
        productId: null,
        summary: null,
        isLoading: true,
        error: '',
    })
    const [reviewSummaryRetryKey, setReviewSummaryRetryKey] = useState(0)
    const [retryKey, setRetryKey] = useState(0)
    const [activeTab, setActiveTab] = useState<ProductDetailTab>(() => {
        const requestedTab = new URLSearchParams(location.search).get('tab')
        return requestedTab === 'reviews' || requestedTab === 'qna'
            ? requestedTab
            : 'information'
    })
    const reviewLoadMoreControllerRef = useRef<AbortController | null>(null)

    useEffect(() => {
        const controller = new AbortController()
        if (invalidProductId) return () => controller.abort()
        reviewLoadMoreControllerRef.current?.abort()
        reviewLoadMoreControllerRef.current = null

        getProduct(id, controller.signal)
            .then((data) => {
                setError('')
                setProduct(data)
                setSelectedImage(data.images[0]?.imageUrl ?? data.thumbnailUrl ?? '')
            })
            .catch((requestError: unknown) => {
                if (requestError instanceof Error && requestError.name !== 'AbortError') setError(requestError.message)
            })
        getProductReviews(id, 1, 10, controller.signal)
            .then((reviewPage) => {
                setReviews(reviewPage.content)
                setReviewCount(reviewPage.totalElements)
                setHasMoreReviews(reviewPage.hasNext)
                setReviewNextPage(2)
                setReviewError('')
                setIsLoadingReviews(false)
            })
            .catch((requestError: unknown) => {
                if (requestError instanceof Error && requestError.name !== 'AbortError') {
                    setReviews([])
                    setReviewCount(0)
                    setHasMoreReviews(false)
                    setReviewNextPage(2)
                    setIsLoadingReviews(false)
                    setReviewError(requestError instanceof ApiError ? requestError.message : '리뷰를 불러오지 못했습니다.')
                }
            })
        return () => {
            controller.abort()
            reviewLoadMoreControllerRef.current?.abort()
            reviewLoadMoreControllerRef.current = null
        }
    }, [id, invalidProductId, retryKey])

    useEffect(() => {
        const controller = new AbortController()
        if (invalidProductId) return () => controller.abort()

        getProductReviewSummary(id, controller.signal)
            .then((summary) => {
                setReviewSummaryState({
                    productId: id,
                    summary,
                    isLoading: false,
                    error: '',
                })
            })
            .catch((requestError: unknown) => {
                if (requestError instanceof Error && requestError.name !== 'AbortError') {
                    setReviewSummaryState((current) => ({
                        productId: id,
                        summary: current.productId === id ? current.summary : null,
                        isLoading: false,
                        error: requestError instanceof ApiError
                            ? requestError.message
                            : 'AI 리뷰 요약을 불러오지 못했습니다.',
                    }))
                }
            })

        return () => controller.abort()
    }, [id, invalidProductId, reviewSummaryRetryKey])

    const discountedPrice = useMemo(() => product ? getDiscountedPrice(product.price, product.discountPercentage) : 0, [product])
    const handleWishlistError = useCallback((message: string) => {
        showToast(message, 'error')
    }, [showToast])
    const handleWishlistChanged = useCallback((message: string) => {
        showToast(message, 'success')
    }, [showToast])

    async function handleAddToCart() {
        if (!product) return
        if (!isAuthenticated) {
            navigate('/login', {
                state: { from: `${location.pathname}${location.search}` },
            })
            return
        }

        setIsAddingToCart(true)
        try {
            await addCartItem({ productId: product.productId, quantity })
            showToast('장바구니에 상품을 담았습니다.', 'success')
        } catch (requestError) {
            showToast(
                requestError instanceof ApiError
                    ? requestError.message
                    : '장바구니에 상품을 담지 못했습니다.',
                'error',
            )
        } finally {
            setIsAddingToCart(false)
        }
    }

    async function loadMoreReviews() {
        if (!product || !hasMoreReviews || isLoadingReviews) return
        const controller = new AbortController()
        reviewLoadMoreControllerRef.current?.abort()
        reviewLoadMoreControllerRef.current = controller
        setReviewError('')
        setIsLoadingReviews(true)
        try {
            const response = await getProductReviews(product.productId, reviewNextPage, 10, controller.signal)
            setReviews((current) => [...current, ...response.content])
            setHasMoreReviews(response.hasNext)
            setReviewNextPage((current) => current + 1)
        } catch (requestError) {
            if (requestError instanceof Error && requestError.name === 'AbortError') return
            setReviewError(
                requestError instanceof ApiError
                    ? requestError.message
                    : '리뷰를 더 불러오지 못했습니다.',
            )
        } finally {
            if (reviewLoadMoreControllerRef.current === controller) {
                reviewLoadMoreControllerRef.current = null
                setIsLoadingReviews(false)
            }
        }
    }

    function retryReviewSummary() {
        setReviewSummaryState((current) => ({
            ...current,
            isLoading: true,
            error: '',
        }))
        setReviewSummaryRetryKey((value) => value + 1)
    }

    if (invalidProductId) return <PageState variant="error" title="상품을 찾을 수 없습니다" description="잘못된 상품 주소입니다." action={<Link className="border border-ink bg-white px-5 py-2.5 text-xs font-bold" to="/">상품 목록으로</Link>} />
    if (error) return <PageState variant="error" title="상품을 불러오지 못했습니다" description={error} action={<button className="border border-ink bg-white px-5 py-2.5 text-xs font-bold" type="button" onClick={() => { setError(''); setProduct(null); setRetryKey((value) => value + 1) }}>다시 시도</button>} />
    if (!product) return <PageState variant="loading" title="상품 정보를 불러오는 중입니다" description="잠시만 기다려 주세요." />

    const detailImages = product.detailImages ?? []

    return (
        <section className="mx-auto max-w-360 px-4 pt-12 pb-20 min-[601px]:px-[clamp(20px,5vw,72px)] min-[601px]:pt-18 min-[601px]:pb-27.5">
            <Link className="mb-7 inline-flex items-center gap-1 text-xs" to="/"><ChevronLeft className="size-4" /> 상품 목록</Link>
            <div className="grid grid-cols-1 gap-10 min-[901px]:grid-cols-[minmax(0,1.1fr)_minmax(360px,.9fr)] min-[901px]:gap-[clamp(40px,7vw,110px)]">
                <div className="min-w-0">
                    <div className="aspect-square overflow-hidden bg-[#e9e9e3]">{selectedImage ? <img className="size-full object-cover" src={resolveImageUrl(selectedImage)} alt={product.name} /> : <div className="grid size-full place-items-center bg-linear-to-br from-[#ebeae4] to-[#d8d9cf] font-serif text-lg font-bold tracking-[.2em] text-[#a2a298]">YMALL</div>}</div>
                    {product.images.length > 0 && <div className="mt-3 flex gap-2.5 overflow-x-auto" aria-label="상품 이미지 선택">{product.images.map((image, index) => <button className={`size-18.5 shrink-0 border p-0 ${selectedImage === image.imageUrl ? 'border-ink' : 'border-transparent'}`} aria-label={`${index + 1}번 상품 이미지 보기`} aria-current={selectedImage === image.imageUrl ? 'true' : undefined} onClick={() => setSelectedImage(image.imageUrl)} key={image.imageId} type="button"><img className="size-full object-cover" src={resolveImageUrl(image.imageUrl)} alt="" /></button>)}</div>}
                </div>
                <div className="pt-3">
                    <div className="inline-block bg-lime px-2.5 py-1 text-[10px] font-extrabold tracking-[.08em]">{product.category.name}</div>
                    <p className="mt-7 mb-1.5 text-[11px] font-extrabold tracking-[.08em] text-muted uppercase">{product.brand}</p>
                    <h1 className="my-2 font-serif text-[clamp(34px,4vw,52px)] leading-[1.05] font-medium tracking-[-.04em]">{product.name}</h1>
                    <div className="flex items-center gap-1 text-[13px]"><Star className="size-4 text-[#8ca324]" fill="currentColor" /> {product.rating?.toFixed(1) ?? '0.0'} <span className="ml-1 text-muted">상품 평점</span></div>
                    <div className="my-8 flex items-baseline gap-2.5">{product.discountPercentage > 0 && <><del className="text-[#aaa]">{formatPrice(product.price)}</del><strong className="text-[#849b21]">{product.discountPercentage}%</strong></>}<b className="ml-auto text-2xl">{formatPrice(discountedPrice)}</b></div>
                    <dl className="m-0 border-y border-line py-4.5 text-[13px]"><div className="grid grid-cols-[70px_1fr] py-2"><dt className="text-muted">배송</dt><dd className="m-0">{product.freeShipping ? '무료배송' : formatPrice(product.shippingFee)} · 평균 {product.estimatedDeliveryDays}일 소요</dd></div><div className="grid grid-cols-[70px_1fr] py-2"><dt className="text-muted">재고</dt><dd className="m-0">{product.stock > 0 ? `${product.stock}개 남음` : '품절'}</dd></div></dl>
                    <div className="flex items-center justify-between py-6 text-[13px]"><span>수량</span><div className="flex items-center border border-line"><button className="grid h-9 w-9.5 place-items-center border-0 bg-transparent" onClick={() => setQuantity((value) => Math.max(1, value - 1))} type="button"><Minus className="size-3.5" /></button><b className="min-w-8.5 text-center">{quantity}</b><button className="grid h-9 w-9.5 place-items-center border-0 bg-transparent disabled:opacity-35" onClick={() => setQuantity((value) => Math.min(product.stock, value + 1))} disabled={product.stock === 0} type="button"><Plus className="size-3.5" /></button></div></div>
                    <div className="grid grid-cols-1 gap-2 min-[601px]:grid-cols-[120px_1fr]">
                        <ProductWishlistButton
                            productId={product.productId}
                            isAuthenticated={isAuthenticated}
                            onError={handleWishlistError}
                            onChanged={handleWishlistChanged}
                            onLoginRequired={() => navigate('/login', {
                                state: { from: `${location.pathname}${location.search}` },
                            })}
                        />
                        <button className="h-13.5 border border-ink bg-ink font-extrabold text-white disabled:border-[#ddd] disabled:bg-[#ddd] disabled:text-[#888]" disabled={isAddingToCart || product.stock === 0 || product.status !== 'APPROVED'} onClick={handleAddToCart} type="button">{product.stock === 0 || product.status === 'SOLD_OUT' ? '품절된 상품입니다' : product.status !== 'APPROVED' ? '구매할 수 없는 상품입니다' : isAddingToCart ? '장바구니에 담는 중...' : `${formatPrice(discountedPrice * quantity)} · 장바구니 담기`}</button>
                    </div>
                    <div className="mt-5.5 flex gap-6.5 text-[11px] text-muted"><span className="flex items-center gap-1.5"><Truck className="size-4" /> {product.freeShipping ? '무료 배송' : `배송비 ${formatPrice(product.shippingFee)}`}</span><span className="flex items-center gap-1.5"><ShieldCheck className="size-4" /> 안전 결제</span></div>
                </div>
            </div>
            <div className="mt-20 grid grid-cols-3 border-y border-line bg-paper" role="tablist" aria-label="상품 상세 메뉴">
                <button className={`h-16 border-r border-line text-sm font-extrabold ${activeTab === 'information' ? 'bg-ink text-surface' : 'bg-paper text-ink'}`} id="product-information-tab" type="button" role="tab" aria-selected={activeTab === 'information'} aria-controls="product-information-panel" onClick={() => setActiveTab('information')}>상품정보</button>
                <button className={`h-16 border-r border-line text-sm font-extrabold ${activeTab === 'reviews' ? 'bg-ink text-surface' : 'bg-paper text-ink'}`} id="product-reviews-tab" type="button" role="tab" aria-selected={activeTab === 'reviews'} aria-controls="product-reviews-panel" onClick={() => setActiveTab('reviews')}>리뷰 <span className="font-normal opacity-65">({reviewCount})</span></button>
                <button className={`h-16 text-sm font-extrabold ${activeTab === 'qna' ? 'bg-ink text-surface' : 'bg-paper text-ink'}`} id="product-qna-tab" type="button" role="tab" aria-selected={activeTab === 'qna'} aria-controls="product-qna-panel" onClick={() => setActiveTab('qna')}>Q&amp;A</button>
            </div>

            {activeTab === 'information' && (
                <div className="pt-10" id="product-information-panel" role="tabpanel" aria-labelledby="product-information-tab">
                    <div className="mx-auto mb-10 max-w-215 border-b border-line pb-8">
                        <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">PRODUCT INFORMATION</p>
                        <h2 className="font-serif text-4xl tracking-tight">상품정보</h2>
                        {product.description && <p className="mt-5 whitespace-pre-wrap text-sm leading-7 text-muted">{product.description}</p>}
                    </div>
                    {detailImages.length > 0 ? (
                        <div className="mx-auto max-w-215 overflow-hidden bg-white">
                            {detailImages.map((image, index) => (
                                <img
                                    className="block h-auto w-full"
                                    src={resolveImageUrl(image.imageUrl)}
                                    alt={`${product.name} 상세 이미지 ${index + 1}`}
                                    loading="lazy"
                                    key={image.detailImageId}
                                />
                            ))}
                        </div>
                    ) : (
                        <PageState variant="empty" title="등록된 상세 이미지가 없습니다" description="상품 설명과 상단 이미지를 참고해 주세요." compact />
                    )}
                </div>
            )}

            {activeTab === 'reviews' && <div className="border-t border-ink pt-9" id="product-reviews-panel" role="tabpanel" aria-labelledby="product-reviews-tab">
                <div className="mb-8 flex flex-wrap items-end justify-between gap-3">
                    <div>
                        <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">REVIEWS</p>
                        <h2 className="font-serif text-4xl tracking-tight">상품 리뷰</h2>
                    </div>
                    <p className="text-sm text-muted">총 {reviewCount}개의 리뷰</p>
                </div>
                <ReviewSummaryPanel
                    summary={reviewSummaryState.productId === id ? reviewSummaryState.summary : null}
                    isLoading={reviewSummaryState.productId !== id || reviewSummaryState.isLoading}
                    error={reviewSummaryState.productId === id ? reviewSummaryState.error : ''}
                    onRetry={retryReviewSummary}
                />
                {reviewError && reviews.length > 0 && <FeedbackMessage className="mb-5" tone="error">{reviewError}</FeedbackMessage>}
                {reviews.length === 0 ? (
                    reviewError
                        ? <PageState variant="error" title="리뷰를 불러오지 못했습니다" description={reviewError} action={<button className="border border-ink bg-white px-4 py-2 text-xs font-bold" type="button" onClick={() => { setReviewError(''); setRetryKey((value) => value + 1) }}>다시 시도</button>} compact />
                        : <PageState variant="empty" title="아직 작성된 리뷰가 없습니다" description="구매한 고객의 첫 리뷰를 기다리고 있습니다." compact />
                ) : (
                    <div className="border-t border-line">
                        {reviews.map((review) => (
                            <article className="grid gap-3 border-b border-line py-6 min-[701px]:grid-cols-[180px_1fr]" key={review.reviewId}>
                                <div>
                                    <strong className="block text-sm">{review.authorName}</strong>
                                    <span className="mt-1 block text-xs text-muted">
                                        {formatKoreanDate(review.createdAt)}
                                    </span>
                                </div>
                                <div>
                                    <div className="mb-3 text-sm tracking-wider text-[#849b21]" aria-label={`평점 ${review.rating}점`}>
                                        {'★'.repeat(review.rating)}<span className="text-[#d8d8d0]">{'★'.repeat(5 - review.rating)}</span>
                                    </div>
                                    <p className="whitespace-pre-wrap text-sm leading-7 text-[#55554f]">{review.content}</p>
                                </div>
                            </article>
                        ))}
                    </div>
                )}
                {hasMoreReviews && (
                    <button
                        className="mx-auto mt-8 block h-11 border border-ink bg-white px-7 text-xs font-bold disabled:opacity-50"
                        type="button"
                        disabled={isLoadingReviews}
                        onClick={loadMoreReviews}
                    >
                        {isLoadingReviews ? '불러오는 중...' : '리뷰 더 보기'}
                    </button>
                )}
            </div>}

            {activeTab === 'qna' && (
                <div id="product-qna-panel" role="tabpanel" aria-labelledby="product-qna-tab">
                    <ProductQuestionSection
                        productId={product.productId}
                        isAuthenticated={isAuthenticated}
                        onLoginRequired={() => navigate('/login', {
                            state: { from: `${location.pathname}${location.search}` },
                        })}
                        onSuccess={() => undefined}
                    />
                </div>
            )}
        </section>
    )
}
