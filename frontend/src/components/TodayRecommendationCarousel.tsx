import { ArrowUpRight, ChevronLeft, ChevronRight, Gift } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { ProductSummary } from '../types/product'
import { formatPrice, getDiscountedPrice, resolveImageUrl } from '../utils/product'

export function TodayRecommendationCarousel({ products }: { products: ProductSummary[] }) {
    const recommendedProducts = products.slice(0, 3)
    const [activeIndex, setActiveIndex] = useState(0)
    const [isPaused, setIsPaused] = useState(false)

    useEffect(() => {
        if (isPaused || recommendedProducts.length < 2) {
            return
        }
        const intervalId = window.setInterval(() => {
            setActiveIndex((index) => (index + 1) % recommendedProducts.length)
        }, 6_500)
        return () => window.clearInterval(intervalId)
    }, [isPaused, recommendedProducts.length])

    if (recommendedProducts.length === 0) {
        return null
    }

    const showPrevious = () => {
        setActiveIndex((index) => (index - 1 + recommendedProducts.length) % recommendedProducts.length)
    }
    const showNext = () => {
        setActiveIndex((index) => (index + 1) % recommendedProducts.length)
    }

    return (
        <section
            className="overflow-hidden bg-[#f0f1eb] py-16 text-[#171717] min-[601px]:py-24"
            aria-roledescription="carousel"
            aria-label="오늘의 추천 아이템"
            onMouseEnter={() => setIsPaused(true)}
            onMouseLeave={() => setIsPaused(false)}
            onFocusCapture={() => setIsPaused(true)}
            onBlurCapture={() => setIsPaused(false)}
        >
            <div className="mx-auto max-w-360 px-4 min-[601px]:px-[clamp(24px,6vw,88px)]">
                <div className="mb-9 flex items-end justify-between">
                    <div>
                        <span className="text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">FOCUS ON</span>
                        <h2 className="mt-2 font-serif text-[clamp(38px,5vw,62px)] leading-none">시선집중</h2>
                        <p className="mt-3 text-sm text-[#686962]">오늘 눈여겨볼 YMall의 추천 상품</p>
                    </div>
                    <div className="flex items-center gap-2">
                        <button className="inline-grid size-11 place-items-center rounded-full border border-[#171717]/20 disabled:opacity-35" type="button" aria-label="이전 추천 상품" disabled={recommendedProducts.length < 2} onClick={showPrevious}>
                            <ChevronLeft className="size-4" aria-hidden="true" />
                        </button>
                        <button className="inline-grid size-11 place-items-center rounded-full border border-[#171717]/20 disabled:opacity-35" type="button" aria-label="다음 추천 상품" disabled={recommendedProducts.length < 2} onClick={showNext}>
                            <ChevronRight className="size-4" aria-hidden="true" />
                        </button>
                    </div>
                </div>

                <div className="overflow-hidden">
                    <div
                        className="flex transition-transform duration-600 ease-[cubic-bezier(.22,.61,.36,1)]"
                        style={{ transform: `translateX(-${(activeIndex % recommendedProducts.length) * 100}%)` }}
                    >
                        {recommendedProducts.map((product, index) => {
                            const productPrice = getDiscountedPrice(product.price, product.discountPercentage)
                            return (
                                <article className="min-w-full" aria-hidden={activeIndex % recommendedProducts.length !== index} key={product.productId}>
                                    <Link
                                        className="group grid min-h-112 overflow-hidden rounded-[26px] bg-[#171717] text-white shadow-[0_24px_60px_rgba(25,25,20,.16)] min-[801px]:grid-cols-[1.2fr_.8fr]"
                                        to={`/products/${product.productId}`}
                                        tabIndex={activeIndex % recommendedProducts.length === index ? 0 : -1}
                                    >
                                        <div className="relative min-h-80 overflow-hidden bg-[#ddd]">
                                            {product.thumbnailUrl ? (
                                                <img className="absolute inset-0 size-full object-cover transition-transform duration-700 group-hover:scale-[1.03]" src={resolveImageUrl(product.thumbnailUrl)} alt={product.name} />
                                            ) : (
                                                <div className="absolute inset-0 grid place-items-center bg-[radial-gradient(circle_at_30%_25%,#d9ff43,transparent_22%),linear-gradient(135deg,#efeee8,#c8cabc)] font-serif text-5xl font-bold tracking-[.2em] text-[#8d8e84]">
                                                    YMALL
                                                </div>
                                            )}
                                            <span className="absolute top-6 left-6 rounded-full bg-white px-4 py-2 text-[10px] font-extrabold tracking-[.14em] text-[#171717]">
                                                FOCUS {String(index + 1).padStart(2, '0')}
                                            </span>
                                        </div>

                                        <div className="flex flex-col justify-center p-8 min-[601px]:p-12">
                                            <span className="text-[11px] font-extrabold tracking-[.14em] text-[#d9ff43] uppercase">{product.brand}</span>
                                            <h3 className="mt-5 font-serif text-[clamp(34px,4vw,58px)] leading-[1.02]">{product.name}</h3>
                                            <div className="mt-7 flex items-center gap-2 text-xs text-white/65">
                                                <Gift className="size-4 text-[#d9ff43]" aria-hidden="true" />
                                                오늘의 추천 상품 특별 혜택
                                            </div>
                                            <div className="mt-5 flex flex-wrap items-baseline gap-3">
                                                {product.discountPercentage > 0 && <strong className="text-2xl text-[#d9ff43]">{product.discountPercentage}%</strong>}
                                                <b className="text-2xl">{formatPrice(productPrice)}</b>
                                                {product.discountPercentage > 0 && <del className="text-xs text-white/45">{formatPrice(product.price)}</del>}
                                            </div>
                                            <span className="mt-10 inline-flex w-fit items-center gap-2 border-b border-white/45 pb-2 text-xs font-extrabold">
                                                상품 자세히 보기
                                                <ArrowUpRight className="size-4" aria-hidden="true" />
                                            </span>
                                        </div>
                                    </Link>
                                </article>
                            )
                        })}
                    </div>
                </div>

                <div className="mt-6 grid grid-cols-3 gap-2 min-[601px]:gap-4" aria-label="추천 상품 슬라이드">
                    {recommendedProducts.map((product, index) => (
                        <button
                            className={`min-w-0 border-t-2 px-1 pt-3 text-left transition-colors ${activeIndex % recommendedProducts.length === index ? 'border-[#171717]' : 'border-[#c9cac2] text-[#777870]'}`}
                            type="button"
                            aria-label={`${index + 1}번 추천 상품 보기`}
                            aria-current={activeIndex % recommendedProducts.length === index ? 'true' : undefined}
                            key={product.productId}
                            onClick={() => setActiveIndex(index)}
                        >
                            <span className="block truncate text-[10px] font-extrabold tracking-[.1em]">{product.brand}</span>
                            <span className="mt-1 block truncate text-xs font-bold min-[601px]:text-sm">{product.name}</span>
                        </button>
                    ))}
                </div>
            </div>
        </section>
    )
}
