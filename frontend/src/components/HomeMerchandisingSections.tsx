import {
    ChevronLeft,
    ChevronRight,
    PackageOpen,
    Pause,
    Play,
    RefreshCw,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getHomeMerchandising } from '../api/home'
import { getCarouselSlideMotionClass, useCarousel } from '../hooks/useCarousel'
import type {
    HomeMerchandising,
    HomeMerchandisingGroup,
    HomeMerchandisingProduct,
} from '../types/home'
import { formatPrice, getDiscountedPrice, resolveImageUrl } from '../utils/product'

interface MerchandisingSlide {
    key: string
    label: string
    products: HomeMerchandisingProduct[]
}

interface MerchandisingCarouselProps {
    ariaLabel: string
    eyebrow: string
    heading: string
    description: string
    slides: MerchandisingSlide[]
    surfaceClassName: string
}

function ProductImage({ product }: { product: HomeMerchandisingProduct }) {
    const [failed, setFailed] = useState(false)
    const imageUrl = product.thumbnailUrl ? resolveImageUrl(product.thumbnailUrl) : null

    if (!imageUrl || failed) {
        return (
            <div className="grid size-full place-items-center bg-linear-to-br from-line to-paper font-serif text-lg font-bold tracking-[.18em] text-muted">
                YMALL
            </div>
        )
    }

    return (
        <img
            className="size-full object-cover transition-transform duration-500 group-hover:scale-[1.03]"
            src={imageUrl}
            alt={product.name}
            onError={() => setFailed(true)}
        />
    )
}

function MerchandisingProductCard({
    product,
    isActive,
}: {
    product: HomeMerchandisingProduct
    isActive: boolean
}) {
    const discountedPrice = getDiscountedPrice(product.price, product.discountPercentage)

    return (
        <Link
            className="group mx-auto grid w-full max-w-260 min-w-0 overflow-hidden rounded-3xl border border-line bg-surface shadow-[0_18px_45px_rgba(20,20,16,.08)] min-[701px]:grid-cols-[minmax(240px,320px)_minmax(0,1fr)]"
            to={`/products/${product.productId}`}
            tabIndex={isActive ? 0 : -1}
        >
            <div className="h-56 overflow-hidden bg-paper min-[701px]:h-72">
                <ProductImage product={product} />
            </div>
            <div className="flex min-w-0 flex-col justify-center p-6 min-[901px]:p-9">
                <span className="truncate text-[10px] font-extrabold tracking-[.14em] text-muted uppercase">
                    {product.brand}
                </span>
                <h3 className="mt-3 line-clamp-2 font-serif text-[clamp(24px,3vw,38px)] leading-tight">
                    {product.name}
                </h3>
                <span className="mt-3 text-xs text-muted">{product.categoryName}</span>
                <div className="mt-5 flex flex-wrap items-baseline gap-2">
                    {product.discountPercentage > 0 && (
                        <strong className="text-lg text-accent">{product.discountPercentage}%</strong>
                    )}
                    <b className="text-lg">{formatPrice(discountedPrice)}</b>
                    {product.discountPercentage > 0 && (
                        <del className="text-[11px] text-muted">{formatPrice(product.price)}</del>
                    )}
                </div>
            </div>
        </Link>
    )
}

function MerchandisingCarousel({
    ariaLabel,
    eyebrow,
    heading,
    description,
    slides,
    surfaceClassName,
}: MerchandisingCarouselProps) {
    const {
        activeIndex,
        previousIndex,
        direction,
        canNavigate,
        isReducedMotion,
        isUserPaused,
        showPrevious,
        showNext,
        showSlide,
        toggleUserPaused,
        finishTransition,
        interactionProps,
    } = useCarousel({ slideCount: slides.length, intervalMs: 6_500 })

    if (slides.length === 0) {
        return null
    }

    return (
        <section
            className={`touch-pan-y overflow-hidden py-16 min-[601px]:py-24 ${surfaceClassName}`}
            aria-roledescription="carousel"
            aria-label={ariaLabel}
            {...interactionProps}
        >
            <div className="mx-auto max-w-360 px-4 min-[601px]:px-[clamp(24px,6vw,88px)]">
                <header className="mb-9 flex items-end justify-between gap-5">
                    <div>
                        <span className="text-[11px] font-extrabold tracking-[.18em] text-accent">
                            {eyebrow}
                        </span>
                        <h2 className="mt-2 font-serif text-[clamp(36px,5vw,58px)] leading-none">{heading}</h2>
                        <p className="mt-3 max-w-150 text-sm text-muted">{description}</p>
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                        <span className="mr-2 text-[11px] font-extrabold tracking-[.12em] text-muted" aria-live="polite">
                            {String(activeIndex + 1).padStart(2, '0')} / {String(slides.length).padStart(2, '0')}
                        </span>
                        <button className="inline-grid size-10 place-items-center rounded-full border border-line disabled:opacity-35" type="button" aria-label={`이전 ${ariaLabel}`} disabled={!canNavigate} onClick={showPrevious}>
                            <ChevronLeft className="size-4" aria-hidden="true" />
                        </button>
                        <button className="inline-grid size-10 place-items-center rounded-full border border-line disabled:opacity-35" type="button" aria-label={`다음 ${ariaLabel}`} disabled={!canNavigate} onClick={showNext}>
                            <ChevronRight className="size-4" aria-hidden="true" />
                        </button>
                        <button
                            className="hidden size-10 place-items-center rounded-full border border-line disabled:opacity-35 min-[601px]:inline-grid"
                            type="button"
                            aria-label={isUserPaused ? `${ariaLabel} 자동 재생` : `${ariaLabel} 자동 재생 일시 정지`}
                            aria-pressed={isUserPaused}
                            disabled={!canNavigate}
                            onClick={toggleUserPaused}
                        >
                            {isUserPaused ? <Play className="size-4" aria-hidden="true" /> : <Pause className="size-4" aria-hidden="true" />}
                        </button>
                    </div>
                </header>

                <div className="overflow-hidden">
                    <div className="relative min-h-90" data-carousel-direction={direction}>
                        {slides.map((slide, index) => (
                            <article
                                className={`${getCarouselSlideMotionClass({ index, activeIndex, previousIndex, direction, isReducedMotion })} min-h-90 w-full`}
                                aria-hidden={activeIndex !== index}
                                aria-label={`${index + 1} / ${slides.length}`}
                                aria-roledescription="slide"
                                key={slide.key}
                                onAnimationEnd={index === activeIndex ? finishTransition : undefined}
                            >
                                <div className={`grid gap-4 ${slide.products.length > 1 ? 'min-[1001px]:grid-cols-2' : ''}`}>
                                    {slide.products.map((product) => (
                                        <MerchandisingProductCard
                                            product={product}
                                            isActive={activeIndex === index}
                                            key={product.productId}
                                        />
                                    ))}
                                </div>
                            </article>
                        ))}
                    </div>
                </div>

                <div className="mt-6 flex items-center justify-center gap-2" aria-label={`${ariaLabel} 슬라이드 선택`}>
                    {slides.map((slide, index) => (
                        <button
                            className={`size-2.5 shrink-0 rounded-full border-0 p-0 transition-[background-color,transform] ${activeIndex === index ? 'scale-110 bg-ink' : 'bg-line hover:bg-muted'}`}
                            type="button"
                            aria-label={`${slide.label} 보기`}
                            aria-current={activeIndex === index ? 'true' : undefined}
                            key={slide.key}
                            disabled={!canNavigate}
                            onClick={() => showSlide(index)}
                        />
                    ))}
                </div>
            </div>
        </section>
    )
}

function groupSlides(groups: HomeMerchandisingGroup[]): MerchandisingSlide[] {
    return groups
        .filter((group) => group.products.length > 0)
        .map((group) => ({
            key: `category-${group.categoryId}`,
            label: group.categoryName,
            products: group.products,
        }))
}

function pairedGroupSlides(groups: HomeMerchandisingGroup[]): MerchandisingSlide[] {
    const populatedGroups = groups.filter((group) => group.products.length > 0)

    return Array.from({ length: Math.ceil(populatedGroups.length / 2) }, (_, index) => {
        const pairedGroups = populatedGroups.slice(index * 2, index * 2 + 2)

        return {
            key: `category-pair-${pairedGroups.map((group) => group.categoryId).join('-')}`,
            label: pairedGroups.map((group) => group.categoryName).join(' · '),
            products: pairedGroups.flatMap((group) => group.products),
        }
    })
}

function pairedProductSlides(products: HomeMerchandisingProduct[], prefix: string): MerchandisingSlide[] {
    return Array.from({ length: Math.ceil(products.length / 2) }, (_, index) => {
        const pairedProducts = products.slice(index * 2, index * 2 + 2)

        return {
            key: `${prefix}-${pairedProducts.map((product) => product.productId).join('-')}`,
            label: pairedProducts.map((product) => product.categoryName).join(' · '),
            products: pairedProducts,
        }
    })
}

function hasMerchandisingData(data: HomeMerchandising) {
    return data.categoryBest.some((group) => group.products.length > 0)
        || data.grocery.some((group) => group.products.length > 0)
        || data.fashion.some((group) => group.products.length > 0)
        || data.newArrivals.length > 0
}

export function HomeMerchandisingSections() {
    const [retryKey, setRetryKey] = useState(0)
    const [state, setState] = useState<{
        requestKey: number
        data: HomeMerchandising | null
        error: string
    }>({ requestKey: -1, data: null, error: '' })

    useEffect(() => {
        const controller = new AbortController()

        getHomeMerchandising(controller.signal)
            .then((data) => setState({ requestKey: retryKey, data, error: '' }))
            .catch((error: unknown) => {
                if (error instanceof Error && error.name !== 'AbortError') {
                    setState({ requestKey: retryKey, data: null, error: error.message })
                }
            })

        return () => controller.abort()
    }, [retryKey])

    const loading = state.requestKey !== retryKey
    const sections = useMemo(() => {
        if (!state.data) {
            return null
        }
        return {
            categoryBest: pairedGroupSlides(state.data.categoryBest),
            grocery: groupSlides(state.data.grocery),
            fashion: groupSlides(state.data.fashion),
            newArrivals: pairedProductSlides(state.data.newArrivals, 'new'),
        }
    }, [state.data])

    if (loading) {
        return (
            <section className="bg-surface py-18" aria-label="홈 상품 큐레이션 로딩" aria-busy="true">
                <div className="mx-auto max-w-360 px-4 min-[601px]:px-[clamp(24px,6vw,88px)]">
                    <div className="h-4 w-28 animate-pulse rounded bg-line" />
                    <div className="mt-4 h-12 w-64 animate-pulse rounded bg-line" />
                    <div className="mt-10 h-80 animate-pulse rounded-3xl bg-line" />
                </div>
            </section>
        )
    }

    if (state.error) {
        return (
            <section className="bg-surface py-18" aria-label="홈 상품 큐레이션 오류">
                <div className="mx-auto flex max-w-360 flex-col items-center px-4 text-center">
                    <PackageOpen className="size-8 text-muted" aria-hidden="true" />
                    <h2 className="mt-4 font-serif text-2xl">추천 상품을 불러오지 못했습니다</h2>
                    <p className="mt-2 text-sm text-muted">잠시 후 다시 시도해 주세요. 다른 메뉴는 계속 이용할 수 있습니다.</p>
                    <button className="mt-5 inline-flex items-center gap-2 rounded-full border border-line px-5 py-2.5 text-xs font-bold" type="button" onClick={() => setRetryKey((value) => value + 1)}>
                        <RefreshCw className="size-4" aria-hidden="true" />
                        다시 시도
                    </button>
                </div>
            </section>
        )
    }

    if (!state.data || !sections || !hasMerchandisingData(state.data)) {
        return (
            <section className="bg-surface py-18 text-center" aria-label="홈 상품 큐레이션 빈 결과">
                <h2 className="font-serif text-2xl">새로운 상품을 준비하고 있습니다</h2>
                <p className="mt-2 text-sm text-muted">곧 다양한 추천 상품으로 찾아올게요.</p>
            </section>
        )
    }

    return (
        <>
            <MerchandisingCarousel
                ariaLabel="카테고리 베스트"
                eyebrow="CATEGORY BEST"
                heading="카테고리 베스트"
                description="각 카테고리에서 가장 많은 선택을 받은 상품을 모았습니다."
                slides={sections.categoryBest}
                surfaceClassName="bg-surface"
            />
            <MerchandisingCarousel
                ariaLabel="오늘의 장보기"
                eyebrow="GROCERY PICK"
                heading="오늘의 장보기"
                description="식품 카테고리별 인기 상품으로 오늘의 장바구니를 채워보세요."
                slides={sections.grocery}
                surfaceClassName="bg-paper"
            />
            <MerchandisingCarousel
                ariaLabel="패션 에디트"
                eyebrow="STYLE CURATION"
                heading="패션 에디트"
                description="패션 카테고리별 베스트 아이템을 한눈에 만나보세요."
                slides={sections.fashion}
                surfaceClassName="bg-surface"
            />
            <MerchandisingCarousel
                ariaLabel="새로 들어온 상품"
                eyebrow="NEW ARRIVALS"
                heading="새로 들어온 상품"
                description="최근 승인된 YMall의 새로운 상품을 가장 먼저 확인하세요."
                slides={sections.newArrivals}
                surfaceClassName="bg-paper"
            />
        </>
    )
}
