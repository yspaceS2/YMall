import { Navigate, Route, Routes } from 'react-router-dom'
import { lazyNamed } from '../utils/lazyNamed'

const NotificationPage = lazyNamed(
    () => import('../pages/NotificationPage'),
    (module) => module.NotificationPage,
)
const OrderDetailPage = lazyNamed(
    () => import('../pages/OrderDetailPage'),
    (module) => module.OrderDetailPage,
)
const OrderHistoryPage = lazyNamed(
    () => import('../pages/OrderHistoryPage'),
    (module) => module.OrderHistoryPage,
)
const MyPage = lazyNamed(() => import('../pages/MyPage'), (module) => module.MyPage)
const SupportInquiryDetailPage = lazyNamed(
    () => import('../pages/SupportInquiryDetailPage'),
    (module) => module.SupportInquiryDetailPage,
)
const SupportInquiryListPage = lazyNamed(
    () => import('../pages/SupportInquiryListPage'),
    (module) => module.SupportInquiryListPage,
)

export function MemberPortalRoutes() {
    return (
        <Routes>
            <Route index element={<MyPage />} />
            <Route path="profile" element={<MyPage />} />
            <Route path="email" element={<MyPage />} />
            <Route path="social" element={<MyPage />} />
            <Route path="wishlist" element={<MyPage />} />
            <Route path="addresses" element={<MyPage />} />
            <Route path="orders" element={<OrderHistoryPage />} />
            <Route path="orders/:orderId" element={<OrderDetailPage />} />
            <Route path="notifications" element={<NotificationPage />} />
            <Route path="support" element={<SupportInquiryListPage />} />
            <Route path="support/:inquiryId" element={<SupportInquiryDetailPage />} />
            <Route path="seller-application" element={<MyPage />} />
            <Route path="*" element={<Navigate to="/mypage" replace />} />
        </Routes>
    )
}
