import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { lazyNamed } from '../utils/lazyNamed'
import { PortalPage } from './PortalPage'

const SettlementManagementPanel = lazyNamed(
    () => import('../components/seller/SettlementManagementPanel'),
    (module) => module.SettlementManagementPanel,
)
const NotificationPage = lazyNamed(
    () => import('../pages/NotificationPage'),
    (module) => module.NotificationPage,
)
const SellerDashboardPage = lazyNamed(
    () => import('../pages/SellerDashboardPage'),
    (module) => module.SellerDashboardPage,
)
const SellerOrderDetailPage = lazyNamed(
    () => import('../pages/SellerOrderDetailPage'),
    (module) => module.SellerOrderDetailPage,
)
const SellerOrderListPage = lazyNamed(
    () => import('../pages/SellerOrderListPage'),
    (module) => module.SellerOrderListPage,
)
const SellerProductEditorPage = lazyNamed(
    () => import('../pages/SellerProductEditorPage'),
    (module) => module.SellerProductEditorPage,
)
const SellerProductListPage = lazyNamed(
    () => import('../pages/SellerProductListPage'),
    (module) => module.SellerProductListPage,
)
const SellerProductQuestionDetailPage = lazyNamed(
    () => import('../pages/SellerProductQuestionDetailPage'),
    (module) => module.SellerProductQuestionDetailPage,
)
const SellerProductQuestionListPage = lazyNamed(
    () => import('../pages/SellerProductQuestionsPage'),
    (module) => module.SellerProductQuestionListPage,
)
const SellerProfilePage = lazyNamed(
    () => import('../pages/SellerProfilePage'),
    (module) => module.SellerProfilePage,
)
const SellerReturnRequestDetailPage = lazyNamed(
    () => import('../pages/SellerReturnRequestDetailPage'),
    (module) => module.SellerReturnRequestDetailPage,
)
const SellerReturnRequestsPage = lazyNamed(
    () => import('../pages/SellerReturnRequestsPage'),
    (module) => module.SellerReturnRequestsPage,
)
const SettlementRequestDetailPage = lazyNamed(
    () => import('../pages/SettlementRequestDetailPage'),
    (module) => module.SettlementRequestDetailPage,
)
const SupportInquiryDetailPage = lazyNamed(
    () => import('../pages/SupportInquiryDetailPage'),
    (module) => module.SupportInquiryDetailPage,
)
const SupportInquiryListPage = lazyNamed(
    () => import('../pages/SupportInquiryListPage'),
    (module) => module.SupportInquiryListPage,
)

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
