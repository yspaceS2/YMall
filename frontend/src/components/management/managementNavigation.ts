import {
    Bell,
    BriefcaseBusiness,
    ClipboardCheck,
    Heart,
    Headphones,
    LayoutDashboard,
    Mail,
    MapPin,
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
    type LucideIcon,
} from 'lucide-react'
import type { AdminPermission } from '../../types/admin'

export type ManagementRole = 'member' | 'seller' | 'admin'

export interface ManagementNavigationItem {
    label: string
    href: string
    icon: LucideIcon
    permission?: AdminPermission
}

const navigationByRole: Record<ManagementRole, ManagementNavigationItem[]> = {
    member: [
        { label: '대시보드', href: '/mypage', icon: LayoutDashboard },
        { label: '회원정보', href: '/mypage/profile', icon: UserRound },
        { label: '이메일 변경', href: '/mypage/email', icon: Mail },
        { label: '소셜 계정', href: '/mypage/social', icon: Users },
        { label: '찜한 상품', href: '/mypage/wishlist', icon: Heart },
        { label: '배송지 관리', href: '/mypage/addresses', icon: MapPin },
        { label: '주문·배송 조회', href: '/mypage/orders', icon: ReceiptText },
        { label: '고객센터', href: '/mypage/support', icon: Headphones },
        { label: '알림', href: '/mypage/notifications', icon: Bell },
        { label: '판매자 신청', href: '/mypage/seller-application', icon: BriefcaseBusiness },
    ],
    seller: [
        { label: '대시보드', href: '/seller', icon: LayoutDashboard },
        { label: '판매자 정보', href: '/seller/profile', icon: Store },
        { label: '상품 관리', href: '/seller/products', icon: PackageCheck },
        { label: '주문·배송 관리', href: '/seller/orders', icon: ReceiptText },
        { label: '반품 관리', href: '/seller/returns', icon: Undo2 },
        { label: '상품 문의 관리', href: '/seller/questions', icon: MessageSquareText },
        { label: '고객센터', href: '/seller/support', icon: Headphones },
        { label: '알림', href: '/seller/notifications', icon: Bell },
        { label: '정산 관리', href: '/seller/settlement', icon: WalletCards },
    ],
    admin: [
        { label: '대시보드', href: '/admin', icon: LayoutDashboard, permission: 'DASHBOARD_READ' },
        { label: '회원 관리', href: '/admin/members', icon: Users, permission: 'MEMBER_READ' },
        { label: '판매자 관리', href: '/admin/sellers', icon: Store, permission: 'SELLER_READ' },
        {
            label: '판매자 신청 관리',
            href: '/admin/seller-applications',
            icon: ClipboardCheck,
            permission: 'SELLER_APPLICATION_REVIEW',
        },
        { label: '상품 승인 관리', href: '/admin/products', icon: PackageSearch, permission: 'PRODUCT_REVIEW' },
        { label: '카테고리 관리', href: '/admin/categories', icon: Tags, permission: 'CATEGORY_READ' },
        { label: '주문 관리', href: '/admin/orders', icon: ReceiptText, permission: 'REFUND_STANDARD' },
        { label: '고객센터 관리', href: '/admin/support', icon: Headphones, permission: 'SUPPORT_REPLY' },
        { label: '알림', href: '/admin/notifications', icon: Bell },
        { label: '정산 관리', href: '/admin/settlement', icon: WalletCards, permission: 'SETTLEMENT_REVIEW' },
    ],
}

export function getManagementNavigation(role: ManagementRole) {
    return navigationByRole[role]
}
