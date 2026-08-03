import {
    Bell,
    Heart,
    LogOut,
    Menu,
    ReceiptText,
    RotateCcw,
    Search,
    ShieldCheck,
    ShoppingCart,
    Store,
    UserRound,
    X,
} from 'lucide-react'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getCategories } from '../api/products'
import ymallSymbolDark from '../assets/brand/ymall-symbol-dark.svg'
import ymallSymbolLight from '../assets/brand/ymall-symbol-light.svg'
import { useTheme } from '../theme/useTheme'
import { useProductSearchSuggestions } from '../hooks/useProductSearchSuggestions'
import type { MemberRole } from '../types/auth'
import type { Category } from '../types/product'
import { CategoryDrawer } from './CategoryDrawer'
import { ThemeSelector } from './ThemeSelector'

interface StoreHeaderProps {
    isAuthenticated: boolean
    role: MemberRole | null
    unreadCount: number
    cartItemCount: number
    onLogout: () => Promise<void>
}

const accountMenuItemClass = 'flex items-center gap-3 px-4 py-3 text-sm transition-colors hover:bg-paper focus:bg-paper focus:outline-none'
export function StoreHeader({
    isAuthenticated,
    role,
    unreadCount,
    cartItemCount,
    onLogout,
}: StoreHeaderProps) {
    const navigate = useNavigate()
    const { resolvedTheme } = useTheme()
    const searchButtonRef = useRef<HTMLButtonElement>(null)
    const searchInputRef = useRef<HTMLInputElement>(null)
    const searchPanelRef = useRef<HTMLDivElement>(null)
    const [searchKeyword, setSearchKeyword] = useState('')
    const [categories, setCategories] = useState<Category[]>([])
    const [isCategoryMenuOpen, setIsCategoryMenuOpen] = useState(false)
    const [isSearchOpen, setIsSearchOpen] = useState(false)
    const [activeSuggestionIndex, setActiveSuggestionIndex] = useState(-1)
    const {
        normalizedKeyword: normalizedSearchKeyword,
        suggestions,
        isLoading: isSuggestionLoading,
        hasError: suggestionError,
        shouldShowSuggestions,
    } = useProductSearchSuggestions(searchKeyword, isSearchOpen)

    useEffect(() => {
        if (isSearchOpen) {
            searchInputRef.current?.focus()
        }
    }, [isSearchOpen])

    useEffect(() => {
        if (!isSearchOpen) return

        const closeOnOutsidePointer = (event: PointerEvent) => {
            const target = event.target
            if (!(target instanceof Node)) return
            if (searchButtonRef.current?.contains(target)) return
            if (searchPanelRef.current?.contains(target)) return
            setIsSearchOpen(false)
        }

        document.addEventListener('pointerdown', closeOnOutsidePointer)
        return () => document.removeEventListener('pointerdown', closeOnOutsidePointer)
    }, [isSearchOpen])

    useEffect(() => {
        if (!isCategoryMenuOpen || categories.length > 0) {
            return
        }

        const controller = new AbortController()
        getCategories(controller.signal)
            .then(setCategories)
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setCategories([])
            })
        return () => controller.abort()
    }, [categories.length, isCategoryMenuOpen])

    const searchByKeyword = (keyword: string) => {
        const trimmedKeyword = keyword.trim()
        navigate(trimmedKeyword ? `/?keyword=${encodeURIComponent(trimmedKeyword)}` : '/')
        setSearchKeyword(trimmedKeyword)
        setIsCategoryMenuOpen(false)
        setIsSearchOpen(false)
        setActiveSuggestionIndex(-1)
    }

    const submitSearch = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        searchByKeyword(searchKeyword)
    }

    const closeMenus = () => {
        setIsCategoryMenuOpen(false)
        setIsSearchOpen(false)
    }

    const handleLogout = async () => {
        closeMenus()
        await onLogout()
    }

    return (
        <header className="sticky top-0 z-30 border-b border-line bg-paper/97 backdrop-blur-md">
            <div className="mx-auto flex h-17 max-w-360 items-center gap-2 px-4 min-[601px]:h-21 min-[601px]:gap-4 min-[601px]:px-[clamp(24px,5vw,72px)]">
                <button
                    className="inline-grid size-11 shrink-0 place-items-center rounded-full border border-line bg-surface transition-colors hover:border-ink"
                    type="button"
                    aria-label={isCategoryMenuOpen ? '헤더 카테고리 메뉴 닫기' : '전체 카테고리 열기'}
                    aria-expanded={isCategoryMenuOpen}
                    aria-controls="store-category-menu"
                    onClick={() => {
                        setIsSearchOpen(false)
                        setIsCategoryMenuOpen((open) => !open)
                    }}
                >
                    <Menu className="size-5" aria-hidden="true" />
                </button>

                <Link className="flex shrink-0 items-center gap-2" to="/" aria-label="YMall 홈" onClick={closeMenus}>
                    <img
                        className="size-11"
                        src={resolvedTheme === 'dark' ? ymallSymbolDark : ymallSymbolLight}
                        alt=""
                        aria-hidden="true"
                    />
                    <span className="hidden font-serif text-xl font-extrabold tracking-[.08em] min-[901px]:inline">
                        YMALL
                    </span>
                </Link>

                <nav className="ml-auto flex shrink-0 items-center gap-1 min-[601px]:gap-2" aria-label="사용자 메뉴">
                    <ThemeSelector />

                    <button
                        ref={searchButtonRef}
                        className="group inline-flex min-w-12 flex-col items-center justify-center gap-1 p-1 text-[10px] font-bold"
                        type="button"
                        aria-label={isSearchOpen ? '검색 닫기' : '검색 열기'}
                        aria-expanded={isSearchOpen}
                        aria-controls="store-search-panel"
                        onClick={() => {
                            setIsCategoryMenuOpen(false)
                            setIsSearchOpen((open) => !open)
                        }}
                    >
                        <Search className="size-5 transition-transform group-hover:scale-110" aria-hidden="true" />
                        <span className="hidden min-[901px]:inline">검색</span>
                    </button>

                    <Link
                        className="group inline-flex min-w-10 flex-col items-center justify-center gap-1 p-1 text-[10px] font-bold"
                        to="/mypage/wishlist"
                        aria-label="찜한 상품"
                        onClick={closeMenus}
                    >
                        <Heart className="size-5 transition-transform group-hover:scale-110" aria-hidden="true" />
                        <span className="hidden min-[901px]:inline">찜</span>
                    </Link>

                    <Link
                        className="group relative inline-flex min-w-12 flex-col items-center justify-center gap-1 p-1 text-[10px] font-bold"
                        to="/cart"
                        aria-label="장바구니"
                        onClick={closeMenus}
                    >
                        <ShoppingCart className="size-5 transition-transform group-hover:scale-110" aria-hidden="true" />
                        <span className="hidden min-[901px]:inline">장바구니</span>
                        {cartItemCount > 0 && (
                            <span
                                className="absolute top-0 right-0 grid min-w-4.5 place-items-center rounded-full bg-accent px-1 text-[9px] leading-4.5 font-extrabold text-paper"
                                aria-label={`장바구니 상품 ${cartItemCount}개`}
                            >
                                {cartItemCount > 99 ? '99+' : cartItemCount}
                            </span>
                        )}
                    </Link>

                    {isAuthenticated ? (
                        <div className="group relative">
                            <Link
                                className="inline-flex min-w-12 flex-col items-center justify-center gap-1 p-1 text-[10px] font-bold"
                                to="/mypage"
                                aria-label="내 정보"
                                aria-haspopup="menu"
                                onClick={closeMenus}
                            >
                                <UserRound className="size-5" aria-hidden="true" />
                                <span className="hidden min-[901px]:inline">내 정보</span>
                            </Link>

                            <div
                                className="absolute top-full right-0 z-50 hidden w-60 pt-2 group-hover:block group-focus-within:block"
                            >
                                <div className="grid border border-line bg-surface py-2 shadow-xl" role="menu" aria-label="내 정보">
                                    <Link className={accountMenuItemClass} to="/mypage/orders" role="menuitem" onClick={closeMenus}>
                                        <ReceiptText className="size-4.5" aria-hidden="true" />
                                        주문·배송 조회
                                    </Link>
                                    <Link className={accountMenuItemClass} to="/mypage/orders" role="menuitem" onClick={closeMenus}>
                                        <RotateCcw className="size-4.5" aria-hidden="true" />
                                        취소·반품·교환
                                    </Link>
                                    <Link className={accountMenuItemClass} to="/mypage/notifications" role="menuitem" onClick={closeMenus}>
                                        <Bell className="size-4.5" aria-hidden="true" />
                                        <span className="flex-1">알림</span>
                                        {unreadCount > 0 && (
                                            <span className="rounded-full bg-accent px-2 py-0.5 text-[10px] font-bold text-paper">
                                                {unreadCount > 99 ? '99+' : unreadCount}
                                            </span>
                                        )}
                                    </Link>
                                    <Link className={accountMenuItemClass} to="/mypage" role="menuitem" onClick={closeMenus}>
                                        <UserRound className="size-4.5" aria-hidden="true" />
                                        회원정보
                                    </Link>
                                    {(role === 'ROLE_SELLER' || role === 'ROLE_ADMIN') && (
                                        <Link className={accountMenuItemClass} to="/seller" role="menuitem" onClick={closeMenus}>
                                            <Store className="size-4.5" aria-hidden="true" />
                                            판매자 센터
                                        </Link>
                                    )}
                                    {role === 'ROLE_ADMIN' && (
                                        <Link className={accountMenuItemClass} to="/admin" role="menuitem" onClick={closeMenus}>
                                            <ShieldCheck className="size-4.5" aria-hidden="true" />
                                            관리자 콘솔
                                        </Link>
                                    )}
                                    <button className={`${accountMenuItemClass} border-t border-line text-left`} type="button" role="menuitem" onClick={handleLogout}>
                                        <LogOut className="size-4.5" aria-hidden="true" />
                                        로그아웃
                                    </button>
                                </div>
                            </div>
                        </div>
                    ) : (
                        <Link
                            className="inline-flex min-w-12 flex-col items-center justify-center gap-1 p-1 text-[10px] font-bold"
                            to="/login"
                            aria-label="로그인"
                            onClick={closeMenus}
                        >
                            <UserRound className="size-5" aria-hidden="true" />
                            <span className="hidden min-[901px]:inline">로그인</span>
                        </Link>
                    )}

                </nav>
            </div>

            {isSearchOpen && (
                <div
                    ref={searchPanelRef}
                    className="absolute top-[calc(100%+8px)] right-4 z-50 w-[min(420px,calc(100vw-32px))] rounded-2xl border border-line bg-surface p-3 shadow-[0_18px_50px_rgba(20,20,16,.18)] min-[601px]:right-[clamp(24px,5vw,72px)]"
                    id="store-search-panel"
                >
                    <form
                        className="flex items-center gap-2"
                        role="search"
                        aria-label="통합 상품 검색"
                        onSubmit={submitSearch}
                    >
                        <div className="flex h-11 min-w-0 flex-1 items-center rounded-xl border border-ink bg-paper px-3">
                            <Search className="mr-2 size-4.5 shrink-0 text-muted" aria-hidden="true" />
                            <input
                                ref={searchInputRef}
                                className="min-w-0 flex-1 border-0 bg-transparent text-sm outline-none placeholder:text-muted"
                                value={searchKeyword}
                                onChange={(event) => {
                                    const nextKeyword = event.target.value
                                    setSearchKeyword(nextKeyword)
                                    setActiveSuggestionIndex(-1)
                                }}
                                onKeyDown={(event) => {
                                    if (event.key === 'Escape') {
                                        setIsSearchOpen(false)
                                        return
                                    }
                                    if (suggestions.length === 0) return
                                    if (event.key === 'ArrowDown') {
                                        event.preventDefault()
                                        setActiveSuggestionIndex((index) => (index + 1) % suggestions.length)
                                    } else if (event.key === 'ArrowUp') {
                                        event.preventDefault()
                                        setActiveSuggestionIndex((index) => index <= 0 ? suggestions.length - 1 : index - 1)
                                    } else if (event.key === 'Enter' && activeSuggestionIndex >= 0) {
                                        event.preventDefault()
                                        searchByKeyword(suggestions[activeSuggestionIndex].name)
                                    }
                                }}
                                placeholder="찾고 싶은 상품을 검색해 보세요"
                                aria-label="상품 검색"
                                role="combobox"
                                aria-autocomplete="list"
                                aria-controls="product-search-suggestions"
                                aria-expanded={normalizedSearchKeyword.length >= 2}
                                aria-activedescendant={activeSuggestionIndex >= 0
                                    ? `product-search-suggestion-${suggestions[activeSuggestionIndex].productId}`
                                    : undefined}
                            />
                            <button className="shrink-0 rounded-lg bg-ink px-3.5 py-2 text-xs font-extrabold text-paper" type="submit" aria-label="상품 검색 실행">
                                검색
                            </button>
                        </div>
                        <button className="inline-grid size-9 shrink-0 place-items-center rounded-full text-muted transition-colors hover:bg-paper hover:text-ink" type="button" aria-label="검색 닫기" onClick={() => setIsSearchOpen(false)}>
                            <X className="size-4" aria-hidden="true" />
                        </button>
                    </form>
                    {shouldShowSuggestions && (
                        <div
                            className="mt-2 overflow-hidden rounded-xl border border-line bg-paper"
                            id="product-search-suggestions"
                            role="listbox"
                            aria-label="추천 검색어"
                        >
                            {isSuggestionLoading && (
                                <p className="px-4 py-3 text-sm text-muted" role="status">추천 검색어를 찾는 중입니다.</p>
                            )}
                            {!isSuggestionLoading && suggestionError && (
                                <p className="px-4 py-3 text-sm text-muted" role="status">추천 검색어를 불러오지 못했습니다.</p>
                            )}
                            {!isSuggestionLoading && !suggestionError && suggestions.length === 0 && (
                                <p className="px-4 py-3 text-sm text-muted" role="status">일치하는 추천 검색어가 없습니다.</p>
                            )}
                            {!isSuggestionLoading && suggestions.map((suggestion, index) => (
                                <button
                                    key={suggestion.productId}
                                    id={`product-search-suggestion-${suggestion.productId}`}
                                    className={`flex w-full items-center gap-3 px-3 py-2.5 text-left text-sm transition-colors ${
                                        index === activeSuggestionIndex ? 'bg-surface' : 'hover:bg-surface'
                                    }`}
                                    type="button"
                                    role="option"
                                    aria-selected={index === activeSuggestionIndex}
                                    onMouseEnter={() => setActiveSuggestionIndex(index)}
                                    onClick={() => searchByKeyword(suggestion.name)}
                                >
                                    {suggestion.thumbnailUrl ? (
                                        <img
                                            className="size-10 shrink-0 rounded-lg object-cover"
                                            src={suggestion.thumbnailUrl}
                                            alt=""
                                        />
                                    ) : (
                                        <span className="grid size-10 shrink-0 place-items-center rounded-lg bg-surface text-muted">
                                            <Search className="size-4" aria-hidden="true" />
                                        </span>
                                    )}
                                    <span className="min-w-0 flex-1 truncate font-semibold">{suggestion.name}</span>
                                    {suggestion.matchType === 'FUZZY' && (
                                        <span className="shrink-0 text-[10px] font-bold text-muted">유사 검색</span>
                                    )}
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            )}

            {isCategoryMenuOpen && (
                <CategoryDrawer categories={categories} isAuthenticated={isAuthenticated} onClose={closeMenus} />
            )}
        </header>
    )
}
