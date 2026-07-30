import {
    ArrowLeft,
    Bell,
    LayoutDashboard,
    LogOut,
    Menu,
    ReceiptText,
    X,
    type LucideIcon,
} from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import ymallSymbolLight from '../../assets/brand/ymall-symbol-light.svg'
import { useAuth } from '../../auth/useAuth'
import { ThemeSelector } from '../ThemeSelector'

type ManagementRole = 'member' | 'seller' | 'admin'

interface NavigationItem {
    label: string
    href: string
    icon: LucideIcon
}

const memberNavigation: NavigationItem[] = [
    { label: '대시보드', href: '/mypage', icon: LayoutDashboard },
    { label: '주문·배송 조회', href: '/mypage/orders', icon: ReceiptText },
    { label: '알림', href: '/mypage/notifications', icon: Bell },
]

const sellerNavigation: NavigationItem[] = [
    { label: '대시보드', href: '/seller', icon: LayoutDashboard },
]

const adminNavigation: NavigationItem[] = [
    { label: '대시보드', href: '/admin', icon: LayoutDashboard },
]

export function ManagementLayout({
    role,
    children,
}: {
    role: ManagementRole
    children: ReactNode
}) {
    const { logout } = useAuth()
    const [isNavigationOpen, setIsNavigationOpen] = useState(false)
    const location = useLocation()
    const isAdmin = role === 'admin'
    const isMember = role === 'member'
    const navigation = isMember
        ? memberNavigation
        : isAdmin
            ? adminNavigation
            : sellerNavigation
    const roleRootPath = isMember ? '/mypage' : `/${role}`
    const centerName = isMember ? '마이페이지' : isAdmin ? '관리자 센터' : '판매자 센터'

    return (
        <div className="min-h-screen bg-paper text-ink">
            <aside
                className={[
                    'fixed inset-y-0 left-0 z-50 flex w-70 flex-col bg-[#20221e] text-white transition-transform duration-200',
                    isNavigationOpen ? 'translate-x-0' : '-translate-x-full',
                    'min-[1001px]:translate-x-0',
                ].join(' ')}
                aria-label={`${centerName} 메뉴`}
            >
                <div className="flex h-22 items-center justify-between border-b border-white/12 px-6">
                    <Link
                        className="flex items-center gap-3"
                        to="/"
                        aria-label="YMall 홈으로 이동"
                    >
                        <img className="size-11" src={ymallSymbolLight} alt="" aria-hidden="true" />
                        <span>
                            <strong className="block font-serif text-xl tracking-[.08em]">YMALL</strong>
                            <span className="mt-0.5 block text-[10px] font-bold tracking-[.16em] text-white/55">
                                {isMember ? 'MY YMALL' : isAdmin ? 'ADMIN OFFICE' : 'SELLER OFFICE'}
                            </span>
                        </span>
                    </Link>
                    <button
                        className="grid size-10 place-items-center min-[1001px]:hidden"
                        type="button"
                        aria-label="관리 메뉴 닫기"
                        onClick={() => setIsNavigationOpen(false)}
                    >
                        <X className="size-5" />
                    </button>
                </div>

                <nav className="flex-1 overflow-y-auto px-4 py-7">
                    <p className="mb-3 px-3 text-[10px] font-extrabold tracking-[.18em] text-white/40">
                        MANAGEMENT
                    </p>
                    <ul className="grid gap-1">
                        {navigation.map((item) => {
                            const Icon = item.icon
                            const isActive = item.href === location.pathname
                                || (item.href !== roleRootPath
                                    && location.pathname.startsWith(`${item.href}/`))
                            const itemClassName = [
                                'flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left text-sm font-bold transition-colors',
                                isActive
                                    ? 'bg-lime text-[#171717]'
                                    : 'text-white/72 hover:bg-white/8 hover:text-white',
                            ].join(' ')
                            return (
                                <li key={item.href}>
                                    <Link
                                        className={itemClassName}
                                        to={item.href}
                                        aria-current={isActive ? 'page' : undefined}
                                        onClick={() => setIsNavigationOpen(false)}
                                    >
                                        <Icon className="size-4.5" aria-hidden="true" />
                                        <span className="flex-1">{item.label}</span>
                                    </Link>
                                </li>
                            )
                        })}
                    </ul>
                </nav>

                <div className="border-t border-white/12 p-4">
                    <Link
                        className="flex items-center gap-3 rounded-lg px-3 py-3 text-sm font-bold text-white/70 transition-colors hover:bg-white/8 hover:text-white"
                        to="/"
                    >
                        <ArrowLeft className="size-4.5" aria-hidden="true" />
                        쇼핑몰로 돌아가기
                    </Link>
                    <button
                        className="mt-1 flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left text-sm font-bold text-white/70 transition-colors hover:bg-white/8 hover:text-white"
                        type="button"
                        onClick={() => void logout()}
                    >
                        <LogOut className="size-4.5" aria-hidden="true" />
                        로그아웃
                    </button>
                </div>
            </aside>

            {isNavigationOpen && (
                <button
                    className="fixed inset-0 z-40 bg-black/45 min-[1001px]:hidden"
                    type="button"
                    aria-label="관리 메뉴 닫기"
                    onClick={() => setIsNavigationOpen(false)}
                />
            )}

            <div className="min-h-screen min-[1001px]:pl-70">
                <header className="sticky top-0 z-30 flex h-22 items-center justify-between border-b border-line bg-surface/95 px-4 backdrop-blur min-[601px]:px-8">
                    <div className="flex items-center gap-3">
                        <button
                            className="grid size-10 place-items-center rounded-full border border-line min-[1001px]:hidden"
                            type="button"
                            aria-label="관리 메뉴 열기"
                            onClick={() => setIsNavigationOpen(true)}
                        >
                            <Menu className="size-5" />
                        </button>
                        <div>
                            <p className="text-[10px] font-extrabold tracking-[.16em] text-muted">
                                YMALL MANAGEMENT
                            </p>
                            <strong className="text-sm">{centerName}</strong>
                        </div>
                    </div>
                    <ThemeSelector />
                </header>
                <main>{children}</main>
            </div>
        </div>
    )
}
