import {
    ArrowLeft,
    Bell,
    BriefcaseBusiness,
    ClipboardCheck,
    Heart,
    LayoutDashboard,
    LogOut,
    Mail,
    MapPin,
    Menu,
    MessageSquareText,
    PackageCheck,
    PackageSearch,
    ReceiptText,
    Store,
    Tags,
    Undo2,
    UserRound,
    Users,
    WalletCards,
    X,
    type LucideIcon,
} from 'lucide-react'
import { useEffect, useState, type ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import {
    getUnreadNotificationCount,
    NOTIFICATIONS_CHANGED_EVENT,
} from '../../api/notifications'
import {
    getSellerPendingQuestionCount,
    SELLER_QUESTION_COUNT_CHANGED_EVENT,
} from '../../api/productQuestions'
import {
    getSellerPendingOrderCount,
    SELLER_PENDING_ORDER_COUNT_CHANGED_EVENT,
} from '../../api/seller'
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
    { label: '회원정보', href: '/mypage/profile', icon: UserRound },
    { label: '이메일 변경', href: '/mypage/email', icon: Mail },
    { label: '소셜 계정', href: '/mypage/social', icon: Users },
    { label: '찜한 상품', href: '/mypage/wishlist', icon: Heart },
    { label: '배송지 관리', href: '/mypage/addresses', icon: MapPin },
    { label: '주문·배송 조회', href: '/mypage/orders', icon: ReceiptText },
    { label: '알림', href: '/mypage/notifications', icon: Bell },
    { label: '판매자 신청', href: '/mypage/seller-application', icon: BriefcaseBusiness },
]

const sellerNavigation: NavigationItem[] = [
    { label: '대시보드', href: '/seller', icon: LayoutDashboard },
    { label: '판매자 정보', href: '/seller/profile', icon: Store },
    { label: '상품 관리', href: '/seller/products', icon: PackageCheck },
    { label: '주문·배송 관리', href: '/seller/orders', icon: ReceiptText },
    { label: '반품 관리', href: '/seller/returns', icon: Undo2 },
    { label: '상품 문의 관리', href: '/seller/questions', icon: MessageSquareText },
    { label: '알림', href: '/seller/notifications', icon: Bell },
    { label: '정산 관리', href: '/seller/settlement', icon: WalletCards },
]

const adminNavigation: NavigationItem[] = [
    { label: '대시보드', href: '/admin', icon: LayoutDashboard },
    { label: '회원 관리', href: '/admin/members', icon: Users },
    { label: '판매자 관리', href: '/admin/sellers', icon: Store },
    {
        label: '판매자 신청 관리',
        href: '/admin/seller-applications',
        icon: ClipboardCheck,
    },
    { label: '상품 승인 관리', href: '/admin/products', icon: PackageSearch },
    { label: '카테고리 관리', href: '/admin/categories', icon: Tags },
    { label: '주문 관리', href: '/admin/orders', icon: ReceiptText },
    { label: '알림', href: '/admin/notifications', icon: Bell },
    { label: '정산 관리', href: '/admin/settlement', icon: WalletCards },
]

export function ManagementLayout({
    role,
    children,
}: {
    role: ManagementRole
    children: ReactNode
}) {
    const { logout, role: authenticatedRole } = useAuth()
    const [isNavigationOpen, setIsNavigationOpen] = useState(false)
    const [pendingQuestionCount, setPendingQuestionCount] = useState(0)
    const [pendingOrderCount, setPendingOrderCount] = useState(0)
    const [unreadNotificationCount, setUnreadNotificationCount] = useState(0)
    const location = useLocation()
    const isAdmin = role === 'admin'
    const isMember = role === 'member'
    const roleNavigation = isMember
        ? memberNavigation
        : isAdmin
            ? adminNavigation
            : sellerNavigation
    const navigation = isMember && authenticatedRole !== 'ROLE_USER'
        ? roleNavigation.filter((item) => item.href !== '/mypage/seller-application')
        : roleNavigation
    const roleRootPath = isMember ? '/mypage' : `/${role}`
    const centerName = isMember ? '마이페이지' : isAdmin ? '관리자 센터' : '판매자 센터'

    useEffect(() => {
        let active = true
        let controller: AbortController | null = null
        const loadUnreadCount = () => {
            controller?.abort()
            controller = new AbortController()
            getUnreadNotificationCount(controller.signal)
                .then((response) => {
                    if (active) setUnreadNotificationCount(response.unreadCount)
                })
                .catch((error: unknown) => {
                    if (error instanceof Error && error.name === 'AbortError') return
                    if (active) setUnreadNotificationCount(0)
                })
        }

        loadUnreadCount()
        const intervalId = window.setInterval(loadUnreadCount, 30_000)
        window.addEventListener(NOTIFICATIONS_CHANGED_EVENT, loadUnreadCount)
        return () => {
            active = false
            controller?.abort()
            window.clearInterval(intervalId)
            window.removeEventListener(NOTIFICATIONS_CHANGED_EVENT, loadUnreadCount)
        }
    }, [])

    useEffect(() => {
        if (role !== 'seller') return
        let active = true
        let controller: AbortController | null = null
        const loadPendingCount = () => {
            controller?.abort()
            controller = new AbortController()
            getSellerPendingQuestionCount(controller.signal)
                .then((response) => {
                    if (active) setPendingQuestionCount(response.count)
                })
                .catch((error: unknown) => {
                    if (error instanceof Error && error.name === 'AbortError') return
                    if (active) setPendingQuestionCount(0)
                })
        }

        loadPendingCount()
        const intervalId = window.setInterval(loadPendingCount, 30_000)
        window.addEventListener(SELLER_QUESTION_COUNT_CHANGED_EVENT, loadPendingCount)
        return () => {
            active = false
            controller?.abort()
            window.clearInterval(intervalId)
            window.removeEventListener(SELLER_QUESTION_COUNT_CHANGED_EVENT, loadPendingCount)
        }
    }, [role])

    useEffect(() => {
        if (role !== 'seller') return
        let active = true
        let controller: AbortController | null = null
        const loadPendingOrderCount = () => {
            controller?.abort()
            controller = new AbortController()
            getSellerPendingOrderCount(controller.signal)
                .then((response) => {
                    if (active) setPendingOrderCount(response.count)
                })
                .catch((error: unknown) => {
                    if (error instanceof Error && error.name === 'AbortError') return
                    if (active) setPendingOrderCount(0)
                })
        }

        loadPendingOrderCount()
        const intervalId = window.setInterval(loadPendingOrderCount, 30_000)
        window.addEventListener(
            SELLER_PENDING_ORDER_COUNT_CHANGED_EVENT,
            loadPendingOrderCount,
        )
        return () => {
            active = false
            controller?.abort()
            window.clearInterval(intervalId)
            window.removeEventListener(
                SELLER_PENDING_ORDER_COUNT_CHANGED_EVENT,
                loadPendingOrderCount,
            )
        }
    }, [role])

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
                            const badgeCount = isOrderMenu
                                ? pendingOrderCount
                                : isQuestionMenu
                                    ? pendingQuestionCount
                                    : isNotificationMenu
                                        ? unreadNotificationCount
                                        : 0
                            const href = isOrderMenu && pendingOrderCount > 0
                                ? '/seller/orders?workType=ACTION_REQUIRED&page=1'
                                : isQuestionMenu && pendingQuestionCount > 0
                                    ? '/seller/questions?status=WAITING&page=1'
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
                    <ThemeSelector />
                </header>
                <main>{children}</main>
            </div>
        </div>
    )
}
