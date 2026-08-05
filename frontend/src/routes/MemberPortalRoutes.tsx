import { Navigate, Route, Routes } from 'react-router-dom'
import { NotificationPage } from '../pages/NotificationPage'
import { OrderDetailPage } from '../pages/OrderDetailPage'
import { OrderHistoryPage } from '../pages/OrderHistoryPage'
import { MyPage } from '../pages/MyPage'
import { SupportInquiryDetailPage } from '../pages/SupportInquiryDetailPage'
import { SupportInquiryListPage } from '../pages/SupportInquiryListPage'

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
