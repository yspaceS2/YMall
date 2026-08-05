import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { SettlementManagementPanel } from '../components/seller/SettlementManagementPanel'
import { NotificationPage } from '../pages/NotificationPage'
import { SellerDashboardPage } from '../pages/SellerDashboardPage'
import { SellerOrderDetailPage } from '../pages/SellerOrderDetailPage'
import { SellerOrderListPage } from '../pages/SellerOrderListPage'
import { SellerProductEditorPage } from '../pages/SellerProductEditorPage'
import { SellerProductListPage } from '../pages/SellerProductListPage'
import { SellerProductQuestionDetailPage } from '../pages/SellerProductQuestionDetailPage'
import { SellerProductQuestionListPage } from '../pages/SellerProductQuestionsPage'
import { SellerProfilePage } from '../pages/SellerProfilePage'
import { SellerReturnRequestDetailPage } from '../pages/SellerReturnRequestDetailPage'
import { SellerReturnRequestsPage } from '../pages/SellerReturnRequestsPage'
import { SettlementRequestDetailPage } from '../pages/SettlementRequestDetailPage'
import { SupportInquiryDetailPage } from '../pages/SupportInquiryDetailPage'
import { SupportInquiryListPage } from '../pages/SupportInquiryListPage'
import { PortalPage } from './PortalPage'

export function SellerPortalRoutes() {
    return (
        <Routes>
            <Route index element={<SellerDashboardPage />} />
            <Route path="profile" element={<SellerProfilePage />} />
            <Route path="products" element={<SellerProductListPage />} />
            <Route path="products/new" element={<SellerProductEditorPage />} />
            <Route path="products/:productId" element={<SellerProductEditorRoute />} />
            <Route path="orders" element={<SellerOrderListPage />} />
            <Route path="orders/:orderId" element={<SellerOrderDetailPage />} />
            <Route path="returns" element={<SellerReturnRequestsPage />} />
            <Route path="returns/:returnRequestId" element={<SellerReturnRequestDetailPage />} />
            <Route path="questions" element={<SellerProductQuestionListPage />} />
            <Route path="questions/:questionId" element={<SellerProductQuestionDetailPage />} />
            <Route path="notifications" element={<NotificationPage />} />
            <Route path="support" element={<SupportInquiryListPage />} />
            <Route path="support/:inquiryId" element={<SupportInquiryDetailPage />} />
            <Route
                path="settlement"
                element={<PortalPage><SettlementManagementPanel /></PortalPage>}
            />
            <Route
                path="settlement/:settlementRequestId"
                element={<SettlementRequestDetailPage role="seller" />}
            />
            <Route path="*" element={<Navigate to="/seller" replace />} />
        </Routes>
    )
}

function SellerProductEditorRoute() {
    const { productId } = useParams()
    const parsedProductId = Number(productId)

    if (!Number.isInteger(parsedProductId) || parsedProductId <= 0) {
        return <Navigate to="/seller/products" replace />
    }

    return <SellerProductEditorPage initialProductId={parsedProductId} />
}
