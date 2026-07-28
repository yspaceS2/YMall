import { ChevronRight, LogIn, UserRound, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { Link } from 'react-router-dom'
import type { Category } from '../types/product'

interface CategoryGroup {
    name: string
    items: string[]
}

interface StoreCategory {
    name: string
    groups: CategoryGroup[]
}

const storeCategories: StoreCategory[] = [
    {
        name: '패션',
        groups: [
            { name: '여성패션', items: ['아우터', '원피스', '상의', '하의'] },
            { name: '남성패션', items: ['아우터', '상의', '팬츠', '정장'] },
            { name: '신발·잡화', items: ['운동화', '구두', '가방', '액세서리'] },
        ],
    },
    {
        name: '뷰티',
        groups: [
            { name: '스킨케어', items: ['스킨·토너', '에센스', '크림', '마스크팩'] },
            { name: '메이크업', items: ['베이스', '립', '아이', '네일'] },
            { name: '헤어·바디', items: ['샴푸', '트리트먼트', '바디케어', '향수'] },
        ],
    },
    {
        name: '식품',
        groups: [
            { name: '신선식품', items: ['과일', '채소', '축산', '수산'] },
            { name: '가공식품', items: ['간편식', '면·통조림', '과자', '음료'] },
            { name: '건강식품', items: ['영양제', '홍삼', '다이어트', '건강즙'] },
        ],
    },
    {
        name: '자동차',
        groups: [
            { name: '차량용품', items: ['인테리어', '전자기기', '안전용품', '수납용품'] },
            { name: '세차·관리', items: ['세차용품', '광택', '정비용품', '타이어'] },
            { name: '오토바이', items: ['헬멧', '보호장비', '부품', '액세서리'] },
        ],
    },
    {
        name: '가구',
        groups: [
            { name: '침실가구', items: ['침대', '매트리스', '옷장', '화장대'] },
            { name: '거실가구', items: ['소파', '테이블', '거실장', '의자'] },
            { name: '수납·서재', items: ['책상', '책장', '선반', '수납장'] },
        ],
    },
    {
        name: '생활',
        groups: [
            { name: '주방', items: ['조리도구', '식기', '보관용기', '주방잡화'] },
            { name: '욕실·청소', items: ['욕실용품', '세탁용품', '청소용품', '휴지'] },
            { name: '반려생활', items: ['강아지용품', '고양이용품', '사료', '위생용품'] },
        ],
    },
    {
        name: '가전',
        groups: [
            { name: '대형가전', items: ['TV', '냉장고', '세탁기', '에어컨'] },
            { name: '주방가전', items: ['전자레인지', '커피머신', '에어프라이어', '믹서기'] },
            { name: '디지털', items: ['노트북', '태블릿', '모니터', '음향기기'] },
        ],
    },
    {
        name: '도서',
        groups: [
            { name: '국내도서', items: ['소설', '경제·경영', '자기계발', '어린이'] },
            { name: '외국도서', items: ['영미도서', '일본도서', '중국도서', '아동도서'] },
            { name: '학습·전문', items: ['수험서', '컴퓨터', '외국어', '대학교재'] },
        ],
    },
]

interface CategoryDrawerProps {
    categories: Category[]
    isAuthenticated: boolean
    onClose: () => void
}

export function CategoryDrawer({ categories, isAuthenticated, onClose }: CategoryDrawerProps) {
    const [activeCategoryIndex, setActiveCategoryIndex] = useState<number | null>(null)
    const [activeGroupIndex, setActiveGroupIndex] = useState<number | null>(null)
    const activeCategory = activeCategoryIndex === null ? null : storeCategories[activeCategoryIndex]
    const activeGroup = activeCategory && activeGroupIndex !== null
        ? activeCategory.groups[activeGroupIndex]
        : null

    useEffect(() => {
        const previousOverflow = document.body.style.overflow
        const closeOnEscape = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                onClose()
            }
        }

        document.body.style.overflow = 'hidden'
        window.addEventListener('keydown', closeOnEscape)
        return () => {
            document.body.style.overflow = previousOverflow
            window.removeEventListener('keydown', closeOnEscape)
        }
    }, [onClose])

    const categoryLink = (name: string) => {
        const registeredCategory = categories.find((category) => category.name === name)
        return registeredCategory
            ? `/?categoryId=${registeredCategory.categoryId}`
            : `/?keyword=${encodeURIComponent(name)}`
    }

    const selectCategory = (index: number) => {
        setActiveCategoryIndex(index)
        setActiveGroupIndex(null)
    }

    const drawerWidthClass = activeGroup
        ? 'w-[min(800px,94vw)]'
        : activeCategory
            ? 'w-[min(430px,94vw)]'
            : 'w-[min(230px,94vw)]'

    return createPortal(
        <div className="fixed inset-0 z-50" id="store-category-menu">
            <button
                className="absolute inset-0 cursor-default bg-black/55"
                type="button"
                aria-label="카테고리 메뉴 닫기"
                onClick={onClose}
            />
            <aside className={`relative flex h-full flex-col bg-surface shadow-2xl transition-[width] duration-200 ${drawerWidthClass}`} aria-label="전체 카테고리">
                <div className="flex min-h-18 items-center justify-between border-b border-line px-4 min-[601px]:px-6">
                    <Link
                        className="inline-flex items-center gap-2 border border-ink px-4 py-2.5 text-sm font-extrabold transition-colors hover:bg-ink hover:text-paper"
                        to={isAuthenticated ? '/mypage' : '/login'}
                        onClick={onClose}
                    >
                        {isAuthenticated
                            ? <UserRound className="size-4" aria-hidden="true" />
                            : <LogIn className="size-4" aria-hidden="true" />}
                        {isAuthenticated ? '내 정보' : '로그인'}
                    </Link>
                    {activeCategory && <strong className="hidden font-serif text-lg tracking-[.08em] min-[601px]:block">ALL CATEGORY</strong>}
                    <button className="inline-grid size-11 place-items-center" type="button" aria-label="전체 카테고리 닫기" onClick={onClose}>
                        <X className="size-6" aria-hidden="true" />
                    </button>
                </div>

                <div className={`grid min-h-0 flex-1 ${
                    activeGroup
                        ? 'grid-cols-[96px_126px_minmax(0,1fr)] min-[601px]:grid-cols-[180px_220px_minmax(0,1fr)]'
                        : activeCategory
                            ? 'grid-cols-[96px_minmax(0,1fr)] min-[601px]:grid-cols-[180px_250px]'
                            : 'grid-cols-1'
                }`}>
                    <nav className="overflow-y-auto border-r border-line bg-paper py-3" aria-label="대분류">
                        <Link className="flex items-center justify-between px-3 py-3 text-xs font-extrabold min-[601px]:px-6 min-[601px]:text-sm" to="/?view=all" onClick={onClose}>
                            전체 상품
                        </Link>
                        {storeCategories.map((category, index) => (
                            <button
                                className={`flex w-full items-center justify-between px-3 py-3 text-left text-xs font-bold transition-colors min-[601px]:px-6 min-[601px]:text-sm ${activeCategoryIndex === index ? 'bg-surface text-[#71801e]' : 'hover:bg-surface'}`}
                                type="button"
                                key={category.name}
                                aria-current={activeCategoryIndex === index ? 'true' : undefined}
                                onMouseEnter={() => selectCategory(index)}
                                onClick={() => selectCategory(index)}
                            >
                                {category.name}
                                <ChevronRight className="size-3.5" aria-hidden="true" />
                            </button>
                        ))}
                    </nav>

                    {activeCategory && (
                        <nav className="overflow-y-auto border-r border-line py-3" aria-label={`${activeCategory.name} 중분류`}>
                            <Link className="block px-3 py-3 text-xs font-extrabold min-[601px]:px-6 min-[601px]:text-sm" to={categoryLink(activeCategory.name)} onClick={onClose}>
                                {activeCategory.name} 전체
                            </Link>
                            {activeCategory.groups.map((group, index) => (
                                <button
                                    className={`flex w-full items-center justify-between px-3 py-3 text-left text-xs transition-colors min-[601px]:px-6 min-[601px]:text-sm ${activeGroupIndex === index ? 'bg-paper font-extrabold' : 'hover:bg-paper'}`}
                                    type="button"
                                    key={group.name}
                                    aria-current={activeGroupIndex === index ? 'true' : undefined}
                                    onMouseEnter={() => setActiveGroupIndex(index)}
                                    onClick={() => setActiveGroupIndex(index)}
                                >
                                    {group.name}
                                    <ChevronRight className="size-3.5" aria-hidden="true" />
                                </button>
                            ))}
                        </nav>
                    )}

                    {activeGroup && (
                        <nav className="overflow-y-auto px-4 py-6 min-[601px]:px-8" aria-label={`${activeGroup.name} 소분류`}>
                            <h2 className="mb-5 font-serif text-2xl font-semibold">{activeGroup.name}</h2>
                            <div className="grid gap-px bg-line">
                                {activeGroup.items.map((item) => (
                                    <Link
                                        className="bg-surface px-3 py-4 text-xs font-bold transition-colors hover:bg-paper min-[601px]:px-5 min-[601px]:text-sm"
                                        to={categoryLink(item)}
                                        key={item}
                                        onClick={onClose}
                                    >
                                        {item}
                                    </Link>
                                ))}
                            </div>
                        </nav>
                    )}
                </div>
            </aside>
        </div>,
        document.body,
    )
}
