import {
    ArrowLeft,
    Bell,
    LogOut,
    Menu,
    X,
} from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import ymallSymbolLight from '../../assets/brand/ymall-symbol-light.svg'
import { useAuth } from '../../auth/useAuth'
import { useOptionalAdminAuthorization } from '../../auth/useAdminAuthorization'
import { ThemeSelector } from '../ThemeSelector'
import { getManagementNavigation, type ManagementRole } from './managementNavigation'
import { useManagementBadgeCounts } from './useManagementBadgeCounts'

export function ManagementLayout({
    role,
    children,
}: {
    role: ManagementRole
    children: ReactNode
}) {
    const { logout, role: authenticatedRole } = useAuth()
    const adminAuthorization = useOptionalAdminAuthorization()
    const [isNavigationOpen, setIsNavigationOpen] = useState(false)
    const location = useLocation()
    const isAdmin = role === 'admin'
    const isMember = role === 'member'
    const roleNavigation = getManagementNavigation(role)
    const navigation = isMember && authenticatedRole !== 'ROLE_USER'
        ? roleNavigation.filter((item) => item.href !== '/mypage/seller-application')
        : isAdmin && adminAuthorization
            ? roleNavigation.filter((item) => !item.permission
                || adminAuthorization.hasPermission(item.permission))
            : roleNavigation
    const roleRootPath = isMember ? '/mypage' : `/${role}`
    const notificationPath = `${roleRootPath}/notifications`
    const centerName = isMember ? '마이페이지' : isAdmin ? '관리자 센터' : '판매자 센터'
    const {
        pendingOrderCount,
        pendingQuestionCount,
        pendingSupportCount,
        unreadNotificationCount,
    } = useManagementBadgeCounts(
        role,
        adminAuthorization?.hasPermission('SUPPORT_REPLY') ?? false,
    )

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
                            const isQuestionMenu = role === 'seller'
                                && item.href === '/seller/questions'
                            const isOrderMenu = role === 'seller'
                                && item.href === '/seller/orders'
                            const isNotificationMenu = item.href.endsWith('/notifications')
                            const isSupportMenu = role === 'admin' && item.href === '/admin/support'
                            const badgeCount = isOrderMenu
                                ? pendingOrderCount
                                : isQuestionMenu
                                    ? pendingQuestionCount
                                    : isSupportMenu
                                        ? pendingSupportCount
                                    : isNotificationMenu
                                        ? unreadNotificationCount
                                        : 0
                            const href = isOrderMenu && pendingOrderCount > 0
                                ? '/seller/orders?workType=ACTION_REQUIRED&page=1'
                                : isQuestionMenu && pendingQuestionCount > 0
                                    ? '/seller/questions?status=WAITING&page=1'
                                    : isSupportMenu && pendingSupportCount > 0
                                        ? '/admin/support?status=WAITING'
                                    : item.href
                            return (
                                <li key={item.href}>
                                    <Link
                                        className={itemClassName}
                                        to={href}
                                        aria-current={isActive ? 'page' : undefined}
                                        onClick={() => setIsNavigationOpen(false)}
                                    >
                                        <Icon className="size-4.5" aria-hidden="true" />
                                        <span className="flex-1">{item.label}</span>
                                        {badgeCount > 0 && (
                                            <span
                                                className={[
                                                    'grid min-w-5.5 place-items-center rounded-full px-1.5 py-0.5 text-[10px] font-extrabold',
                                                    isActive
                                                        ? 'bg-[#171717] text-white'
                                                        : 'bg-lime text-[#171717]',
                                                ].join(' ')}
                                                aria-label={isOrderMenu
                                                    ? `처리 필요 주문 ${badgeCount}건`
                                                    : isQuestionMenu
                                                        ? `답변 대기 문의 ${badgeCount}건`
                                                        : isSupportMenu
                                                            ? `처리 대기 고객센터 문의 ${badgeCount}건`
                                                        : `읽지 않은 알림 ${badgeCount}건`}
                                            >
                                                {badgeCount > 99 ? '99+' : badgeCount}
                                            </span>
                                        )}
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
                    <div className="flex items-center gap-2">
                        <ThemeSelector />
                        <Link
                            className="group relative inline-flex min-w-12 flex-col items-center justify-center gap-1 p-1 text-[10px] font-bold"
                            to={notificationPath}
                            aria-label={unreadNotificationCount > 0
                                ? `헤더 알림, 읽지 않은 알림 ${unreadNotificationCount}건`
                                : '헤더 알림'}
                        >
                            <Bell className="size-5 transition-transform group-hover:scale-110" aria-hidden="true" />
                            <span className="hidden min-[901px]:inline">알림</span>
                            {unreadNotificationCount > 0 && (
                                <span
                                    className="absolute -top-1 right-0 grid min-w-4.5 place-items-center rounded-full bg-lime px-1 py-0.5 text-[9px] font-extrabold text-[#171717]"
                                    aria-hidden="true"
                                >
                                    {unreadNotificationCount > 99 ? '99+' : unreadNotificationCount}
                                </span>
                            )}
                        </Link>
                        <button
                            className="group inline-flex min-w-12 flex-col items-center justify-center gap-1 p-1 text-[10px] font-bold"
                            type="button"
                            aria-label="헤더 로그아웃"
                            onClick={() => void logout()}
                        >
                            <LogOut className="size-5 transition-transform group-hover:scale-110" aria-hidden="true" />
                            <span className="hidden min-[901px]:inline">로그아웃</span>
                        </button>
                    </div>
                </header>
                <main>{children}</main>
            </div>
        </div>
    )
}
