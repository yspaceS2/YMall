import { ArrowRight, ChevronLeft, ChevronRight, Pause, Play, ShoppingBasket, Sparkles } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { getCarouselSlideMotionClass, useCarousel } from '../hooks/useCarousel'

interface EditorialSlide {
    eyebrow: string
    title: string
    description: string
    link: string
    linkLabel: string
    background: string
    visual: ReactNode
}

interface HomeEditorialCarouselProps {
    ariaLabel: string
    badge: ReactNode
    heading: string
    description: string
    slides: EditorialSlide[]
    sectionClassName: string
    headingClassName?: string
    controlClassName?: string
}

function HomeEditorialCarousel({
    ariaLabel,
    badge,
    heading,
    description,
    slides,
    sectionClassName,
    headingClassName = '',
    controlClassName = '',
}: HomeEditorialCarouselProps) {
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
    } = useCarousel({ slideCount: slides.length, intervalMs: 6_000 })

    return (
        <section
            className={`touch-pan-y overflow-hidden py-16 min-[601px]:py-24 ${sectionClassName}`}
            aria-roledescription="carousel"
            aria-label={ariaLabel}
            {...interactionProps}
        >
            <div className="mx-auto max-w-360 px-4 min-[601px]:px-[clamp(24px,6vw,88px)]">
                <header className="mb-9 flex items-end justify-between gap-6">
                    <div>
                        <div className="flex items-center gap-2 text-[11px] font-extrabold tracking-[.16em]">
                            {badge}
                        </div>
                        <h2 className={`mt-3 font-serif text-[clamp(38px,5vw,62px)] leading-none ${headingClassName}`}>{heading}</h2>
                        <p className="mt-3 max-w-150 text-sm opacity-65">{description}</p>
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                        <button className={`inline-grid size-11 place-items-center rounded-full border disabled:opacity-35 ${controlClassName}`} type="button" aria-label={`이전 ${ariaLabel}`} disabled={!canNavigate} onClick={showPrevious}>
                            <ChevronLeft className="size-4" aria-hidden="true" />
                        </button>
                        <button className={`inline-grid size-11 place-items-center rounded-full border disabled:opacity-35 ${controlClassName}`} type="button" aria-label={`다음 ${ariaLabel}`} disabled={!canNavigate} onClick={showNext}>
                            <ChevronRight className="size-4" aria-hidden="true" />
                        </button>
                        <button
                            className={`inline-grid size-11 place-items-center rounded-full border disabled:opacity-35 ${controlClassName}`}
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

                <div className="overflow-hidden rounded-[26px]">
                    <div className="relative min-h-105" data-carousel-direction={direction}>
                        {slides.map((slide, index) => (
                            <article
                                className={`${getCarouselSlideMotionClass({ index, activeIndex, previousIndex, direction, isReducedMotion })} min-h-105 w-full overflow-hidden ${slide.background}`}
                                aria-hidden={activeIndex !== index}
                                aria-label={`${index + 1} / ${slides.length}`}
                                aria-roledescription="slide"
                                key={slide.eyebrow}
                                onAnimationEnd={index === activeIndex ? finishTransition : undefined}
                            >
                                <div className="relative z-10 grid min-h-105 min-[801px]:grid-cols-[.9fr_1.1fr]">
                                    <div className="flex flex-col justify-center p-8 min-[601px]:p-12">
                                        <span className="text-[10px] font-extrabold tracking-[.16em] opacity-65">{slide.eyebrow}</span>
                                        <h3 className="mt-5 max-w-150 whitespace-pre-line font-serif text-[clamp(38px,5vw,66px)] leading-[.98]">{slide.title}</h3>
                                        <p className="mt-6 max-w-115 text-sm leading-6 opacity-70">{slide.description}</p>
                                        <Link className="mt-9 inline-flex w-fit items-center gap-3 border-b border-current pb-2 text-xs font-extrabold" to={slide.link} tabIndex={activeIndex === index ? 0 : -1}>
                                            {slide.linkLabel}
                                            <ArrowRight className="size-4" aria-hidden="true" />
                                        </Link>
                                    </div>
                                    <div className="relative hidden min-[601px]:block">
                                        {slide.visual}
                                    </div>
                                </div>
                            </article>
                        ))}
                    </div>
                </div>

                <div className="mt-5 flex items-center gap-2">
                    {slides.map((slide, index) => (
                        <button
                            className={`h-1 transition-[width,background-color] ${activeIndex === index ? 'w-12 bg-current' : 'w-5 bg-current opacity-25'}`}
                            type="button"
                            aria-label={`${ariaLabel} ${index + 1}번 보기`}
                            aria-current={activeIndex === index ? 'true' : undefined}
                            key={slide.eyebrow}
                            disabled={!canNavigate}
                            onClick={() => showSlide(index)}
                        />
                    ))}
                    <span className="ml-2 text-[10px] font-extrabold tracking-[.12em] opacity-55">
                        {String(activeIndex + 1).padStart(2, '0')} / {String(slides.length).padStart(2, '0')}
                    </span>
                </div>
            </div>
        </section>
    )
}

const grocerySlides: EditorialSlide[] = [
    {
        eyebrow: 'FRESH MARKET 01',
        title: '오늘의 식탁을\n더 신선하게',
        description: '매일 필요한 신선식품과 간편한 한 끼를 한곳에서 만나보세요.',
        link: '/?keyword=식품',
        linkLabel: '신선식품 장보기',
        background: 'bg-[#f4d8a8] text-[#271b10]',
        visual: (
            <>
                <div className="absolute top-[14%] right-[13%] size-[62%] rounded-full bg-[#e95f35]" />
                <div className="absolute right-[42%] bottom-[12%] size-[37%] rounded-full bg-[#f5f0d8]" />
                <div className="absolute right-[11%] bottom-[13%] h-[33%] w-[30%] rounded-t-full bg-[#647633]" />
                <span className="absolute right-[27%] bottom-[24%] rotate-8 font-serif text-[clamp(70px,10vw,150px)] text-[#271b10]">FRESH</span>
            </>
        ),
    },
    {
        eyebrow: 'EASY TABLE 02',
        title: '바쁜 하루에도\n든든한 한 끼',
        description: '간편식부터 홈카페 메뉴까지, 취향대로 채우는 우리 집 식탁.',
        link: '/?keyword=간편식',
        linkLabel: '간편식 둘러보기',
        background: 'bg-[#c9d9d0] text-[#183026]',
        visual: (
            <>
                <div className="absolute top-[9%] right-[9%] h-[76%] w-[54%] rotate-3 rounded-[45%_45%_12%_12%] bg-[#315b48]" />
                <div className="absolute top-[22%] right-[24%] size-[34%] rounded-full border-[18px] border-[#f2e8cf]" />
                <div className="absolute right-[43%] bottom-[4%] h-[57%] w-[8%] -rotate-12 bg-[#d9ff43]" />
            </>
        ),
    },
    {
        eyebrow: 'DAILY STOCK 03',
        title: '생활 필수품을\n한 번에 채우기',
        description: '떨어지기 전에 미리 준비하는 생활용품과 주방용품 셀렉션.',
        link: '/?keyword=생활',
        linkLabel: '생활용품 장보기',
        background: 'bg-[#d8d7ee] text-[#22213c]',
        visual: (
            <>
                <div className="absolute top-[12%] right-[13%] h-[72%] w-[25%] rounded-t-full bg-[#706eb3]" />
                <div className="absolute right-[37%] bottom-[13%] h-[48%] w-[23%] bg-[#f8f6ee]" />
                <div className="absolute top-[20%] right-[52%] size-[22%] rotate-12 border-2 border-[#22213c] bg-[#d9ff43]" />
            </>
        ),
    },
]

const fashionSlides: EditorialSlide[] = [
    {
        eyebrow: 'STYLE EDIT 01',
        title: '가볍게 시작하는\n새로운 실루엣',
        description: '매일 입기 좋은 기본과 지금 필요한 포인트를 함께 제안합니다.',
        link: '/?keyword=패션',
        linkLabel: '패션 컬렉션 보기',
        background: 'bg-[#d6ff4b] text-[#151515]',
        visual: (
            <>
                <div className="absolute top-[8%] right-[17%] h-[84%] w-[31%] -rotate-5 bg-[#181818]" />
                <div className="absolute right-[45%] bottom-[7%] h-[68%] w-[25%] rotate-8 border-2 border-[#181818] bg-[#f5f3ea]" />
                <span className="absolute top-[13%] right-[8%] [writing-mode:vertical-rl] text-[11px] font-extrabold tracking-[.25em]">YMALL STYLE ARCHIVE</span>
            </>
        ),
    },
    {
        eyebrow: 'WEEKEND LOOK 02',
        title: '주말의 온도를\n입는 방법',
        description: '편안한 소재와 여유로운 핏으로 완성하는 주말 스타일.',
        link: '/?keyword=캐주얼',
        linkLabel: '캐주얼 아이템 보기',
        background: 'bg-[#e8d8ca] text-[#30221c]',
        visual: (
            <>
                <div className="absolute top-[11%] right-[16%] h-[78%] w-[42%] rounded-t-full bg-[#9d6247]" />
                <div className="absolute top-[23%] right-[29%] size-[22%] rounded-full bg-[#f1c7b5]" />
                <div className="absolute right-[46%] bottom-[9%] h-[48%] w-[18%] bg-[#30221c]" />
            </>
        ),
    },
    {
        eyebrow: 'ACCESSORY NOTE 03',
        title: '작지만 선명한\n오늘의 포인트',
        description: '가방과 슈즈, 액세서리로 익숙한 룩에 새로운 표정을 더해보세요.',
        link: '/?keyword=액세서리',
        linkLabel: '액세서리 둘러보기',
        background: 'bg-[#b8c8df] text-[#17253a]',
        visual: (
            <>
                <div className="absolute top-[14%] right-[12%] size-[64%] rounded-full border-[26px] border-[#17253a]" />
                <div className="absolute right-[34%] bottom-[5%] h-[50%] w-[31%] rotate-6 bg-[#f4efdf]" />
                <div className="absolute top-[18%] right-[47%] size-[16%] rounded-full bg-[#d9ff43]" />
            </>
        ),
    },
]

export function GroceryEditorialCarousel() {
    return (
        <HomeEditorialCarousel
            ariaLabel="장보기 큐레이션"
            badge={<><ShoppingBasket className="size-4" aria-hidden="true" /> GROCERY PICK</>}
            heading="오늘의 장보기"
            description="식탁부터 생활까지, 자주 찾는 상품을 취향 있게 골랐어요."
            slides={grocerySlides}
            sectionClassName="bg-[#fffaf1] text-[#252019]"
            controlClassName="border-[#252019]/25"
        />
    )
}

export function FashionEditorialCarousel() {
    return (
        <HomeEditorialCarousel
            ariaLabel="패션 큐레이션"
            badge={<><Sparkles className="size-4" aria-hidden="true" /> STYLE CURATION</>}
            heading="패션 에디트"
            description="지금의 분위기를 완성하는 YMall의 시즌 스타일 제안."
            slides={fashionSlides}
            sectionClassName="bg-[#151515] text-[#f5f4ed]"
            headingClassName="text-[#f5f4ed]"
            controlClassName="border-white/25"
        />
    )
}
