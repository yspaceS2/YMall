import { ArrowRight, ChevronLeft, ChevronRight, Pause, Play } from 'lucide-react'
import { Link } from 'react-router-dom'
import automotiveToolsImage from '../assets/home/categories/automotive-tools.jpg'
import beautyImage from '../assets/home/categories/beauty.jpg'
import booksHobbyImage from '../assets/home/categories/books-hobby.jpg'
import digitalAppliancesImage from '../assets/home/categories/digital-appliances.jpg'
import fashionImage from '../assets/home/categories/fashion.jpg'
import foodImage from '../assets/home/categories/food.jpg'
import furnitureInteriorImage from '../assets/home/categories/furniture-interior.jpg'
import livingKitchenImage from '../assets/home/categories/living-kitchen.jpg'
import { getCarouselSlideMotionClass, useCarousel } from '../hooks/useCarousel'

const eventSlides = [
    {
        eyebrow: 'CATEGORY 01 · FASHION',
        title: '새로운 계절의\n패션 컬렉션',
        description: '매일의 옷차림에 자연스럽게 스며드는 아이템을 만나보세요.',
        link: '/?categoryId=2',
        linkLabel: '패션 컬렉션 보기',
        image: fashionImage,
    },
    {
        eyebrow: 'CATEGORY 02 · BEAUTY',
        title: '매일을 채우는\n뷰티 루틴',
        description: '기초 케어부터 메이크업까지 나를 위한 아이템을 모았습니다.',
        link: '/?categoryId=3',
        linkLabel: '뷰티 컬렉션 보기',
        image: beautyImage,
    },
    {
        eyebrow: 'CATEGORY 03 · FOOD',
        title: '신선함을 담은\n오늘의 식탁',
        description: '좋은 재료로 채우는 맛있고 건강한 한 끼를 준비해 보세요.',
        link: '/?categoryId=4',
        linkLabel: '식품 컬렉션 보기',
        image: foodImage,
    },
    {
        eyebrow: 'CATEGORY 04 · INTERIOR',
        title: '머물고 싶은\n나만의 공간',
        description: '취향과 편안함을 담은 가구와 인테리어를 제안합니다.',
        link: '/?categoryId=5',
        linkLabel: '가구·인테리어 보기',
        image: furnitureInteriorImage,
    },
    {
        eyebrow: 'CATEGORY 05 · LIVING',
        title: '일상을 가볍게\n생활의 발견',
        description: '주방부터 생활까지 매일 손이 가는 아이템을 골랐습니다.',
        link: '/?categoryId=6',
        linkLabel: '생활·주방 컬렉션 보기',
        image: livingKitchenImage,
    },
    {
        eyebrow: 'CATEGORY 06 · DIGITAL',
        title: '하루를 바꾸는\n디지털 라이프',
        description: '일과 휴식을 더 편리하게 만드는 디지털 기기를 만나보세요.',
        link: '/?categoryId=7',
        linkLabel: '가전·디지털 컬렉션 보기',
        image: digitalAppliancesImage,
    },
    {
        eyebrow: 'CATEGORY 07 · AUTOMOTIVE',
        title: '차와 작업을 위한\n든든한 도구',
        description: '관리부터 작업까지 믿고 사용할 자동차용품과 공구입니다.',
        link: '/?categoryId=8',
        linkLabel: '자동차·공구 컬렉션 보기',
        image: automotiveToolsImage,
    },
    {
        eyebrow: 'CATEGORY 08 · HOBBY',
        title: '취향을 넓히는\n책과 새로운 취미',
        description: '읽고 만들며 나만의 즐거움과 새로운 취향을 발견해 보세요.',
        link: '/?categoryId=9',
        linkLabel: '도서·취미 컬렉션 보기',
        image: booksHobbyImage,
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
                        className={`${getCarouselSlideMotionClass({ index, activeIndex, previousIndex, direction, isReducedMotion })} min-h-100 w-full overflow-hidden bg-[#f6f1e8] text-[#171717] min-[901px]:min-h-130`}
                        aria-hidden={activeIndex !== index}
                        aria-label={`${index + 1} / ${eventSlides.length}`}
                        aria-roledescription="slide"
                        key={slide.eyebrow}
                        onAnimationEnd={index === activeIndex ? finishTransition : undefined}
                    >
                        <Link
                            className="absolute inset-0 z-10 block text-inherit no-underline focus-visible:outline-2 focus-visible:outline-offset-[-4px] focus-visible:outline-[#171717]"
                            to={slide.link}
                            aria-label={slide.linkLabel}
                            tabIndex={activeIndex === index ? 0 : -1}
                        >
                            <img className="absolute inset-0 size-full object-cover object-[68%_center]" src={slide.image} alt="" aria-hidden="true" />
                            <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(248,246,240,.98)_0%,rgba(248,246,240,.96)_42%,rgba(248,246,240,.42)_67%,rgba(248,246,240,.08)_100%)] min-[901px]:bg-[linear-gradient(90deg,rgba(248,246,240,.98)_0%,rgba(248,246,240,.95)_38%,rgba(248,246,240,.26)_61%,rgba(248,246,240,0)_78%)]" aria-hidden="true" />
                            <div className="relative grid min-h-100 grid-cols-1 min-[901px]:min-h-130 min-[901px]:grid-cols-[1.05fr_.95fr]">
                                <div className="flex max-w-210 flex-col justify-center px-6 py-16 min-[601px]:px-[clamp(40px,8vw,120px)]">
                                    <p className="mb-5 text-[11px] font-extrabold tracking-[.2em]">{slide.eyebrow}</p>
                                    <h1 className="max-w-180 break-keep whitespace-pre-line font-serif text-[clamp(38px,4.5vw,68px)] leading-[1.02] font-medium tracking-[-.045em] text-balance">
                                        {slide.title}
                                    </h1>
                                    <p className="mt-6 max-w-[38ch] break-keep text-sm leading-6 text-pretty opacity-75 min-[601px]:text-base min-[601px]:leading-7">{slide.description}</p>
                                    <span className="mt-8 inline-flex w-fit items-center gap-3 border-b border-current pb-2 text-xs font-extrabold whitespace-nowrap">
                                        {slide.linkLabel}
                                        <ArrowRight className="size-4" aria-hidden="true" />
                                    </span>
                                </div>
                            </div>
                        </Link>
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
