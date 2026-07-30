import { ChevronRight, LogIn, UserRound, X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { createPortal } from 'react-dom'
import { Link } from 'react-router-dom'
import type { Category } from '../types/product'

interface CategoryDrawerProps {
    categories: Category[]
    isAuthenticated: boolean
    onClose: () => void
}

export function CategoryDrawer({ categories, isAuthenticated, onClose }: CategoryDrawerProps) {
    const [activeCategoryId, setActiveCategoryId] = useState<number | null>(null)
    const [activeGroupId, setActiveGroupId] = useState<number | null>(null)
    const rootCategories = useMemo(
        () => getCategoryChildren(categories, null),
        [categories],
    )
    const activeCategory = categories.find(
        (category) => category.categoryId === activeCategoryId,
    ) ?? null
    const groups = useMemo(
        () => getCategoryChildren(categories, activeCategoryId),
        [activeCategoryId, categories],
    )
    const activeGroup = categories.find(
        (category) => category.categoryId === activeGroupId,
    ) ?? null
    const items = useMemo(
        () => getCategoryChildren(categories, activeGroupId),
        [activeGroupId, categories],
    )

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

    const selectCategory = (categoryId: number) => {
        setActiveCategoryId(categoryId)
        setActiveGroupId(null)
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
            <aside
                className={`relative flex h-full flex-col bg-surface shadow-2xl transition-[width] duration-200 ${drawerWidthClass}`}
                aria-label="전체 카테고리"
            >
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
                    {activeCategory && (
                        <strong className="hidden font-serif text-lg tracking-[.08em] min-[601px]:block">
                            ALL CATEGORY
                        </strong>
                    )}
                    <button
                        className="inline-grid size-11 place-items-center"
                        type="button"
                        aria-label="전체 카테고리 닫기"
                        onClick={onClose}
                    >
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
                        <Link
                            className="flex items-center justify-between px-3 py-3 text-xs font-extrabold min-[601px]:px-6 min-[601px]:text-sm"
                            to="/?view=all"
                            onClick={onClose}
                        >
                            전체 상품
                        </Link>
                        {rootCategories.map((category) => (
                            <button
                                className={`flex w-full items-center justify-between px-3 py-3 text-left text-xs font-bold transition-colors min-[601px]:px-6 min-[601px]:text-sm ${
                                    activeCategoryId === category.categoryId
                                        ? 'bg-surface text-[#71801e]'
                                        : 'hover:bg-surface'
                                }`}
                                type="button"
                                key={category.categoryId}
                                aria-current={activeCategoryId === category.categoryId ? 'true' : undefined}
                                onMouseEnter={() => selectCategory(category.categoryId)}
                                onFocus={() => selectCategory(category.categoryId)}
                                onClick={() => selectCategory(category.categoryId)}
                            >
                                {category.name}
                                <ChevronRight className="size-3.5" aria-hidden="true" />
                            </button>
                        ))}
                        {rootCategories.length === 0 && (
                            <p className="px-3 py-4 text-xs text-muted min-[601px]:px-6">
                                등록된 카테고리가 없습니다.
                            </p>
                        )}
                    </nav>

                    {activeCategory && (
                        <nav
                            className="overflow-y-auto border-r border-line py-3"
                            aria-label={`${activeCategory.name} 중분류`}
                        >
                            <Link
                                className="block px-3 py-3 text-xs font-extrabold min-[601px]:px-6 min-[601px]:text-sm"
                                to={categoryLink(activeCategory.categoryId)}
                                onClick={onClose}
                            >
                                {activeCategory.name} 전체
                            </Link>
                            {groups.map((group) => {
                                const hasChildren = categories.some(
                                    (category) => category.parentId === group.categoryId,
                                )

                                if (!hasChildren) {
                                    return (
                                        <Link
                                            className="flex w-full items-center justify-between px-3 py-3 text-left text-xs transition-colors hover:bg-paper min-[601px]:px-6 min-[601px]:text-sm"
                                            to={categoryLink(group.categoryId)}
                                            key={group.categoryId}
                                            onClick={onClose}
                                        >
                                            {group.name}
                                        </Link>
                                    )
                                }

                                return (
                                    <button
                                        className={`flex w-full items-center justify-between px-3 py-3 text-left text-xs transition-colors min-[601px]:px-6 min-[601px]:text-sm ${
                                            activeGroupId === group.categoryId
                                                ? 'bg-paper font-extrabold'
                                                : 'hover:bg-paper'
                                        }`}
                                        type="button"
                                        key={group.categoryId}
                                        aria-current={activeGroupId === group.categoryId ? 'true' : undefined}
                                        onMouseEnter={() => setActiveGroupId(group.categoryId)}
                                        onFocus={() => setActiveGroupId(group.categoryId)}
                                        onClick={() => setActiveGroupId(group.categoryId)}
                                    >
                                        {group.name}
                                        <ChevronRight className="size-3.5" aria-hidden="true" />
                                    </button>
                                )
                            })}
                        </nav>
                    )}

                    {activeGroup && (
                        <nav
                            className="overflow-y-auto px-4 py-6 min-[601px]:px-8"
                            aria-label={`${activeGroup.name} 소분류`}
                        >
                            <h2 className="mb-5 font-serif text-2xl font-semibold">
                                {activeGroup.name}
                            </h2>
                            <div className="grid gap-px bg-line">
                                <Link
                                    className="bg-paper px-3 py-4 text-xs font-extrabold transition-colors hover:bg-surface min-[601px]:px-5 min-[601px]:text-sm"
                                    to={categoryLink(activeGroup.categoryId)}
                                    onClick={onClose}
                                >
                                    {activeGroup.name} 전체
                                </Link>
                                {items.map((item) => (
                                    <Link
                                        className="bg-surface px-3 py-4 text-xs font-bold transition-colors hover:bg-paper min-[601px]:px-5 min-[601px]:text-sm"
                                        to={categoryLink(item.categoryId)}
                                        key={item.categoryId}
                                        onClick={onClose}
                                    >
                                        {item.name}
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

function categoryLink(categoryId: number) {
    return `/?categoryId=${categoryId}`
}

function getCategoryChildren(categories: Category[], parentId: number | null) {
    return categories
        .filter((category) => (category.parentId ?? null) === parentId)
        .sort((left, right) => (
            (left.displayOrder ?? 0) - (right.displayOrder ?? 0)
            || left.name.localeCompare(right.name, 'ko')
        ))
}
