import type { ReactNode } from 'react'
import { Bell, Heart, LogOut, ReceiptText, Search, ShieldCheck, ShoppingBag, Store, UserRound } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getUnreadNotificationCount, NOTIFICATIONS_CHANGED_EVENT } from '../api/notifications'
import { useAuth } from '../auth/useAuth'

export function Layout({ children }: { children: ReactNode }) {
    const { isAuthenticated, role, logout } = useAuth()
    const [unreadCount, setUnreadCount] = useState(0)

    useEffect(() => {
        if (!isAuthenticated) {
            return
        }
        let controller: AbortController | null = null
        const loadUnreadCount = () => {
            controller?.abort()
            controller = new AbortController()
            getUnreadNotificationCount(controller.signal)
                .then((response) => setUnreadCount(response.unreadCount))
                .catch((error: unknown) => {
                    if (error instanceof Error && error.name === 'AbortError') return
                    setUnreadCount(0)
                })
        }
        loadUnreadCount()
        const intervalId = window.setInterval(loadUnreadCount, 30_000)
        window.addEventListener(NOTIFICATIONS_CHANGED_EVENT, loadUnreadCount)
        return () => {
            controller?.abort()
            window.clearInterval(intervalId)
            window.removeEventListener(NOTIFICATIONS_CHANGED_EVENT, loadUnreadCount)
        }
    }, [isAuthenticated])

    const handleLogout = () => {
        setUnreadCount(0)
        logout()
    }

    return (
        <div className="flex min-h-screen flex-col bg-paper text-ink">
            <header className="sticky top-0 z-20 grid h-16 grid-cols-[1fr_auto] items-center border-b border-line bg-paper/95 px-4.5 backdrop-blur-md min-[601px]:h-19 min-[601px]:grid-cols-[1fr_auto_1fr] min-[601px]:px-[clamp(24px,5vw,80px)]">
                <Link className="font-serif text-3xl font-extrabold tracking-[-2px]" to="/" aria-label="YMall 홈">
                    Y<span className="text-[#7d931d]">mall</span>
                </Link>
                <nav className="hidden gap-8.5 text-xs font-extrabold tracking-[.14em] min-[901px]:flex" aria-label="주요 메뉴">
                    <Link to="/">NEW</Link>
                    <Link to="/">BEST</Link>
                    <Link to="/">SHOP</Link>
                    <Link to="/">EVENT</Link>
                </nav>
                <div className="flex items-center justify-self-end gap-4.5" aria-label="사용자 메뉴">
                    <Search className="hidden size-5 min-[601px]:block" aria-hidden="true" />
                    <Heart className="hidden size-5 min-[601px]:block" aria-hidden="true" />
                    <Link className="inline-grid size-5 place-items-center bg-transparent p-0" to="/cart" aria-label="장바구니">
                        <ShoppingBag className="size-5" aria-hidden="true" />
                    </Link>
                    {isAuthenticated ? (
                        <>
                            {(role === 'ROLE_SELLER' || role === 'ROLE_ADMIN') && (
                                <Link className="inline-grid size-5 place-items-center bg-transparent p-0" to="/seller" aria-label="판매자 관리">
                                    <Store className="size-5" aria-hidden="true" />
                                </Link>
                            )}
                            {role === 'ROLE_ADMIN' && (
                                <Link className="inline-grid size-5 place-items-center bg-transparent p-0" to="/admin" aria-label="관리자 운영">
                                    <ShieldCheck className="size-5" aria-hidden="true" />
                                </Link>
                            )}
                            <Link className="inline-grid size-5 place-items-center bg-transparent p-0" to="/orders" aria-label="주문 내역">
                                <ReceiptText className="size-5" aria-hidden="true" />
                            </Link>
                            <Link className="relative inline-grid size-5 place-items-center bg-transparent p-0" to="/notifications" aria-label={`알림 ${unreadCount}개`}>
                                <Bell className="size-5" aria-hidden="true" />
                                {unreadCount > 0 && (
                                    <span className="absolute -right-2.5 -top-2.5 grid min-w-4.5 place-items-center rounded-full bg-[#849b21] px-1 text-[9px] font-bold leading-4.5 text-white">
                                        {unreadCount > 99 ? '99+' : unreadCount}
                                    </span>
                                )}
                            </Link>
                            <button className="inline-grid size-5 place-items-center border-0 bg-transparent p-0" type="button" onClick={handleLogout} aria-label="로그아웃">
                                <LogOut className="size-5" aria-hidden="true" />
                            </button>
                        </>
                    ) : (
                        <Link className="inline-grid size-5 place-items-center bg-transparent p-0" to="/login" aria-label="로그인">
                            <UserRound className="size-5" aria-hidden="true" />
                        </Link>
                    )}
                </div>
            </header>
            <main className="flex-1">{children}</main>
            <footer className="flex flex-col items-start gap-6 bg-[#1d1d1b] px-5 py-10 text-xs text-[#bcbcb5] min-[601px]:px-[clamp(24px,5vw,80px)] min-[601px]:py-12 min-[901px]:flex-row min-[901px]:items-end min-[901px]:justify-between">
                <div><strong className="font-serif text-2xl text-white">YMALL</strong><p className="mt-2.5">일상에 취향을 더하는 셀렉트 스토어</p></div>
                <p>© 2026 YMall. Portfolio project.</p>
            </footer>
        </div>
    )
}
