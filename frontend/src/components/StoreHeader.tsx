import {
    Bell,
    ChevronDown,
    Headphones,
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
} from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getCategories } from '../api/products'
import ymallSymbolDark from '../assets/brand/ymall-symbol-dark.svg'
import ymallSymbolLight from '../assets/brand/ymall-symbol-light.svg'
import { useTheme } from '../theme/useTheme'
import type { MemberRole } from '../types/auth'
import type { Category } from '../types/product'
import { CategoryDrawer } from './CategoryDrawer'
import { ThemeSelector } from './ThemeSelector'

interface StoreHeaderProps {
    isAuthenticated: boolean
    role: MemberRole | null
    unreadCount: number
    onLogout: () => Promise<void>
}

const accountMenuItemClass = 'flex items-center gap-3 px-4 py-3 text-sm transition-colors hover:bg-paper focus:bg-paper focus:outline-none'

export function StoreHeader({
    isAuthenticated,
    role,
    unreadCount,
    onLogout,
}: StoreHeaderProps) {
    const navigate = useNavigate()
    const { resolvedTheme } = useTheme()
    const [searchKeyword, setSearchKeyword] = useState('')
    const [categories, setCategories] = useState<Category[]>([])
    const [isCategoryMenuOpen, setIsCategoryMenuOpen] = useState(false)
    const [isAccountMenuOpen, setIsAccountMenuOpen] = useState(false)

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

    const submitSearch = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        const keyword = searchKeyword.trim()
        navigate(keyword ? `/?keyword=${encodeURIComponent(keyword)}` : '/')
        setIsCategoryMenuOpen(false)
    }

    const closeMenus = () => {
        setIsCategoryMenuOpen(false)
        setIsAccountMenuOpen(false)
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
                        setIsCategoryMenuOpen((open) => !open)
                        setIsAccountMenuOpen(false)
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

                <form
                    className="mx-auto hidden h-12 min-w-0 max-w-160 flex-1 items-center border-2 border-ink bg-surface px-4 min-[601px]:flex"
                    role="search"
                    aria-label="통합 상품 검색"
                    onSubmit={submitSearch}
                >
                    <input
                        className="min-w-0 flex-1 border-0 bg-transparent text-sm outline-none placeholder:text-muted"
                        value={searchKeyword}
                        onChange={(event) => setSearchKeyword(event.target.value)}
                        placeholder="찾고 싶은 상품을 검색해 보세요"
                        aria-label="상품 검색"
                    />
                    <button className="inline-grid size-8 place-items-center border-0 bg-transparent" type="submit" aria-label="검색">
                        <Search className="size-5" aria-hidden="true" />
                    </button>
                </form>

                <nav className="ml-auto flex shrink-0 items-center gap-1 min-[601px]:gap-2" aria-label="사용자 메뉴">
                    <ThemeSelector />

                    <Link
                        className="group inline-flex min-w-10 flex-col items-center justify-center gap-1 p-1 text-[10px] font-bold"
                        to="/mypage#wishlist"
                        aria-label="찜한 상품"
                        onClick={closeMenus}
                    >
                        <Heart className="size-5 transition-transform group-hover:scale-110" aria-hidden="true" />
                        <span className="hidden min-[901px]:inline">찜</span>
                    </Link>

                    {isAuthenticated ? (
                        <div className="group relative">
                            <button
                                className="inline-flex min-w-12 flex-col items-center justify-center gap-1 p-1 text-[10px] font-bold"
                                type="button"
                                aria-label="내 정보 메뉴"
                                aria-haspopup="menu"
                                aria-expanded={isAccountMenuOpen}
                                onClick={() => setIsAccountMenuOpen((open) => !open)}
                            >
                                <span className="flex items-center">
                                    <UserRound className="size-5" aria-hidden="true" />
                                    <ChevronDown className="hidden size-3 min-[901px]:block" aria-hidden="true" />
                                </span>
                                <span className="hidden min-[901px]:inline">내 정보</span>
                            </button>

                            <div
                                className={`${isAccountMenuOpen ? 'grid' : 'hidden'} absolute top-full right-0 z-50 mt-2 w-60 border border-line bg-surface py-2 shadow-xl group-hover:grid group-focus-within:grid`}
                                role="menu"
                                aria-label="내 정보"
                            >
                                <Link className={accountMenuItemClass} to="/orders" role="menuitem" onClick={closeMenus}>
                                    <ReceiptText className="size-4.5" aria-hidden="true" />
                                    주문·배송 조회
                                </Link>
                                <Link className={accountMenuItemClass} to="/orders" role="menuitem" onClick={closeMenus}>
                                    <RotateCcw className="size-4.5" aria-hidden="true" />
                                    취소·반품·교환
                                </Link>
                                <Link className={accountMenuItemClass} to="/notifications" role="menuitem" onClick={closeMenus}>
                                    <Bell className="size-4.5" aria-hidden="true" />
                                    <span className="flex-1">알림</span>
                                    {unreadCount > 0 && (
                                        <span className="rounded-full bg-[#849b21] px-2 py-0.5 text-[10px] font-bold text-white">
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
                                <span className={`${accountMenuItemClass} cursor-not-allowed text-muted`} role="menuitem" aria-disabled="true">
                                    <Headphones className="size-4.5" aria-hidden="true" />
                                    <span className="flex-1">고객센터</span>
                                    <span className="text-[10px]">준비 중</span>
                                </span>
                                <button className={`${accountMenuItemClass} border-t border-line text-left`} type="button" role="menuitem" onClick={handleLogout}>
                                    <LogOut className="size-4.5" aria-hidden="true" />
                                    로그아웃
                                </button>
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

                    <Link
                        className="group inline-flex min-w-12 flex-col items-center justify-center gap-1 p-1 text-[10px] font-bold"
                        to="/cart"
                        aria-label="장바구니"
                        onClick={closeMenus}
                    >
                        <ShoppingCart className="size-5 transition-transform group-hover:scale-110" aria-hidden="true" />
                        <span className="hidden min-[901px]:inline">장바구니</span>
                    </Link>
                </nav>
            </div>

            <form
                className="mx-4 mb-3 flex h-11 items-center border-2 border-ink bg-surface px-3 min-[601px]:hidden"
                role="search"
                aria-label="모바일 통합 상품 검색"
                onSubmit={submitSearch}
            >
                <input
                    className="min-w-0 flex-1 border-0 bg-transparent text-sm outline-none placeholder:text-muted"
                    value={searchKeyword}
                    onChange={(event) => setSearchKeyword(event.target.value)}
                    placeholder="상품을 검색해 보세요"
                    aria-label="모바일 상품 검색"
                />
                <button className="inline-grid size-8 place-items-center border-0 bg-transparent" type="submit" aria-label="모바일 검색">
                    <Search className="size-5" aria-hidden="true" />
                </button>
            </form>

            {isCategoryMenuOpen && (
                <CategoryDrawer categories={categories} isAuthenticated={isAuthenticated} onClose={closeMenus} />
            )}
        </header>
    )
}
