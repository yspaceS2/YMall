import { ArrowRight, ChevronLeft, ChevronRight, Pause, Play } from 'lucide-react'
import { Link } from 'react-router-dom'
import { getCarouselSlideMotionClass, useCarousel } from '../hooks/useCarousel'

const eventSlides = [
    {
        eyebrow: 'SEASON EVENT 01',
        title: '새로운 계절,\n가벼운 옷차림',
        description: '일상에 자연스럽게 스며드는 이번 시즌 패션 셀렉션',
        link: '/?keyword=패션',
        linkLabel: '패션 셀렉션 보기',
        background: 'bg-[#d9ff43] text-[#171717]',
        visual: (
            <div className="relative size-full" aria-hidden="true">
                <div className="absolute top-[10%] right-[12%] h-[72%] w-[42%] rotate-6 bg-[#171717]" />
                <div className="absolute right-[34%] bottom-[9%] h-[62%] w-[34%] -rotate-8 border-2 border-[#171717] bg-[#f8f8f5]" />
                <span className="absolute right-[22%] bottom-[20%] font-serif text-[clamp(80px,13vw,180px)] leading-none text-[#d9ff43]">Y</span>
            </div>
        ),
    },
    {
        eyebrow: 'BEAUTY WEEK',
        title: '나를 위한\n매일의 루틴',
        description: '기초부터 포인트 메이크업까지, 오늘의 뷰티 큐레이션',
        link: '/?keyword=뷰티',
        linkLabel: '뷰티 아이템 보기',
        background: 'bg-[#efd6e4] text-[#2b1721]',
        visual: (
            <div className="relative size-full" aria-hidden="true">
                <div className="absolute top-[10%] right-[16%] size-[58%] rounded-full bg-[#c97298]" />
                <div className="absolute right-[17%] bottom-[10%] h-[62%] w-[18%] rounded-t-full bg-[#2b1721]" />
                <div className="absolute right-[40%] bottom-[12%] h-[48%] w-[22%] border-2 border-[#2b1721] bg-[#fff6fa]" />
            </div>
        ),
    },
    {
        eyebrow: 'HOME REFRESH',
        title: '집 안에 더하는\n작은 변화',
        description: '머무는 시간이 즐거워지는 가구와 생활 아이템',
        link: '/?keyword=가구',
        linkLabel: '홈 컬렉션 보기',
        background: 'bg-[#c9d9d0] text-[#13251d]',
        visual: (
            <div className="relative size-full" aria-hidden="true">
                <div className="absolute right-[10%] bottom-[12%] h-[42%] w-[68%] rounded-t-[48%] bg-[#315b48]" />
                <div className="absolute right-[18%] bottom-[8%] h-[11%] w-[8%] bg-[#13251d]" />
                <div className="absolute right-[61%] bottom-[8%] h-[11%] w-[8%] bg-[#13251d]" />
                <div className="absolute top-[13%] right-[21%] size-[24%] rounded-full border-2 border-[#13251d]" />
            </div>
        ),
    },
]

export function HomeEventCarousel() {
    const {
        activeIndex,
        previousIndex,
        direction,
        canNavigate,
        isReducedMotion,
        isUserPaused,
        showPrevious,
        showNext,
        toggleUserPaused,
        finishTransition,
        interactionProps,
    } = useCarousel({ slideCount: eventSlides.length, intervalMs: 5_500 })

    return (
        <section
            className="relative min-h-100 touch-pan-y overflow-hidden min-[901px]:min-h-130"
            aria-roledescription="carousel"
            aria-label="이벤트 프로모션"
            {...interactionProps}
        >
            <div className="relative min-h-100 min-[901px]:min-h-130" data-carousel-direction={direction}>
                {eventSlides.map((slide, index) => (
                    <article
                        className={`${getCarouselSlideMotionClass({ index, activeIndex, previousIndex, direction, isReducedMotion })} min-h-100 w-full overflow-hidden min-[901px]:min-h-130 ${slide.background}`}
                        aria-hidden={activeIndex !== index}
                        aria-label={`${index + 1} / ${eventSlides.length}`}
                        aria-roledescription="slide"
                        key={slide.eyebrow}
                        onAnimationEnd={index === activeIndex ? finishTransition : undefined}
                    >
                        <div className="relative z-10 grid min-h-100 grid-cols-1 min-[901px]:min-h-130 min-[901px]:grid-cols-[1.05fr_.95fr]">
                            <div className="flex flex-col justify-center px-6 py-16 min-[601px]:px-[clamp(40px,8vw,120px)]">
                                <p className="mb-5 text-[11px] font-extrabold tracking-[.2em]">{slide.eyebrow}</p>
                                <h1 className="max-w-180 whitespace-pre-line font-serif text-[clamp(44px,7vw,86px)] leading-[.94] font-medium tracking-[-.055em]">
                                    {slide.title}
                                </h1>
                                <p className="mt-7 max-w-130 text-sm leading-6 opacity-75 min-[601px]:text-base">{slide.description}</p>
                                <Link className="mt-9 inline-flex w-fit items-center gap-3 border-b border-current pb-2 text-xs font-extrabold" to={slide.link} tabIndex={activeIndex === index ? 0 : -1}>
                                    {slide.linkLabel}
                                    <ArrowRight className="size-4" aria-hidden="true" />
                                </Link>
                            </div>
                            <div className="absolute inset-y-0 right-0 hidden w-[52%] min-[601px]:block min-[901px]:relative min-[901px]:w-auto">
                                {slide.visual}
                            </div>
                        </div>
                    </article>
                ))}
            </div>

            <div className="absolute right-5 bottom-5 z-20 flex items-center gap-2 text-[#171717] min-[601px]:right-10 min-[601px]:bottom-8">
                <button className="inline-grid size-10 place-items-center rounded-full border border-[#171717]/35 bg-white/25 backdrop-blur-sm disabled:opacity-35" type="button" aria-label="이전 이벤트" disabled={!canNavigate} onClick={showPrevious}>
                    <ChevronLeft className="size-4" aria-hidden="true" />
                </button>
                <span className="min-w-14 text-center text-xs font-extrabold">{activeIndex + 1} / {eventSlides.length}</span>
                <button className="inline-grid size-10 place-items-center rounded-full border border-[#171717]/35 bg-white/25 backdrop-blur-sm disabled:opacity-35" type="button" aria-label="다음 이벤트" disabled={!canNavigate} onClick={showNext}>
                    <ChevronRight className="size-4" aria-hidden="true" />
                </button>
                <button
                    className="inline-grid size-10 place-items-center rounded-full border border-[#171717]/35 bg-white/25 backdrop-blur-sm disabled:opacity-35"
                    type="button"
                    aria-label={isUserPaused ? '이벤트 자동 재생' : '이벤트 자동 재생 일시 정지'}
                    aria-pressed={isUserPaused}
                    disabled={!canNavigate}
                    onClick={toggleUserPaused}
                >
                    {isUserPaused ? <Play className="size-4" aria-hidden="true" /> : <Pause className="size-4" aria-hidden="true" />}
                </button>
            </div>
        </section>
    )
}
