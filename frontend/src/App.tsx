import type { ReactNode } from 'react'
import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { RequireAuth } from './auth/RequireAuth'
import { RequireRole } from './auth/RequireRole'
import { AdminAuthorizationProvider } from './auth/AdminAuthorizationProvider'
import { RequireAdminPermission } from './auth/RequireAdminPermission'
import { Layout } from './components/Layout'
import { ManagementLayout } from './components/management/ManagementLayout'
import { AdminSellerApplicationPanel } from './components/admin/AdminSellerApplicationPanel'
import { SettlementManagementPanel } from './components/seller/SettlementManagementPanel'
import { AdminSettlementRequestList } from './components/settlement/SettlementRequestList'
import { CartPage } from './pages/CartPage'
import { CheckoutPage } from './pages/CheckoutPage'
import { LoginPage } from './pages/LoginPage'
import { MyPage } from './pages/MyPage'
import { NotificationPage } from './pages/NotificationPage'
import { OrderDetailPage } from './pages/OrderDetailPage'
import { OrderHistoryPage } from './pages/OrderHistoryPage'
import { OrderResultPage } from './pages/OrderResultPage'
import { PaymentPage } from './pages/PaymentPage'
import { PasswordResetPage } from './pages/PasswordResetPage'
import { ProductDetailPage } from './pages/ProductDetailPage'
import { ProductListPage } from './pages/ProductListPage'
import { SellerDashboardPage } from './pages/SellerDashboardPage'
import { SellerProductEditorPage } from './pages/SellerProductEditorPage'
import { SellerProfilePage } from './pages/SellerProfilePage'
import { SellerOrderDetailPage } from './pages/SellerOrderDetailPage'
import { SellerOrderListPage } from './pages/SellerOrderListPage'
import { SellerProductListPage } from './pages/SellerResourcePages'
import { SignupPage } from './pages/SignupPage'
import { TossPaymentFailPage } from './pages/TossPaymentFailPage'
import { TossPaymentSuccessPage } from './pages/TossPaymentSuccessPage'
import { OAuth2CallbackPage } from './pages/OAuth2CallbackPage'
import { OAuthSignupPage } from './pages/OAuthSignupPage'
import { AdminManagementPage } from './pages/AdminManagementPage'
import { AdminResourceDetailPage } from './pages/AdminResourceDetailPage'
import { AdminResourceListPage } from './pages/AdminResourceListPage'
import { AdminCategoryManagementPage } from './pages/AdminCategoryManagementPage'
import {
    AdminProductReviewDetailPage,
    AdminProductReviewListPage,
} from './pages/AdminProductReviewPage'
import {
    AdminProductChangeReviewDetailPage,
    AdminProductChangeReviewListPage,
} from './pages/AdminProductChangeReviewPage'
import { AccessDeniedPage } from './pages/AccessDeniedPage'
import {
    SellerReturnRequestDetailPage,
    SellerReturnRequestsPage,
} from './pages/SellerReturnRequestsPage'
import {
    SellerProductQuestionDetailPage,
    SellerProductQuestionListPage,
} from './pages/SellerProductQuestionsPage'
import { SettlementRequestDetailPage } from './pages/SettlementRequestDetailPage'
import {
    SupportInquiryDetailPage,
    SupportInquiryListPage,
} from './pages/SupportInquiryPages'
import type { AdminPermission } from './types/admin'

function App() {
    return (
        <Routes>
            <Route
                path="/mypage/*"
                element={
                    <RequireAuth>
                        <ManagementLayout role="member">
                            <MemberPortalRoutes />
                        </ManagementLayout>
                    </RequireAuth>
                }
            />
            <Route
                path="/seller/*"
                element={
                    <RequireRole roles={['ROLE_SELLER', 'ROLE_ADMIN']}>
                        <ManagementLayout role="seller">
                            <SellerPortalRoutes />
                        </ManagementLayout>
                    </RequireRole>
                }
            />
            <Route
                path="/admin/*"
                element={
                    <RequireRole roles={['ROLE_ADMIN']}>
                        <AdminAuthorizationProvider>
                            <ManagementLayout role="admin">
                                <AdminPortalRoutes />
                            </ManagementLayout>
                        </AdminAuthorizationProvider>
                    </RequireRole>
                }
            />
            <Route path="*" element={<Layout><StoreRoutes /></Layout>} />
        </Routes>
    )
}

function MemberPortalRoutes() {
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

function SellerPortalRoutes() {
    return (
        <Routes>
            <Route index element={<SellerDashboardPage />} />
            <Route path="profile" element={<SellerProfilePage />} />
            <Route path="products" element={<SellerProductListPage />} />
            <Route
                path="products/new"
                element={<SellerProductEditorPage />}
            />
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

function AdminPortalRoutes() {
    return (
        <Routes>
            <Route index element={withAdminPermission(
                <AdminManagementPage />,
                'DASHBOARD_READ',
            )} />
            <Route path="members" element={withAdminPermission(
                <AdminResourceListPage resource="members" />,
                'MEMBER_READ',
            )} />
            <Route
                path="members/:resourceId"
                element={withAdminPermission(
                    <AdminResourceDetailPage resource="members" />,
                    'MEMBER_READ',
                )}
            />
            <Route path="sellers" element={withAdminPermission(
                <AdminResourceListPage resource="sellers" />,
                'SELLER_READ',
            )} />
            <Route
                path="sellers/:resourceId"
                element={withAdminPermission(
                    <AdminResourceDetailPage resource="sellers" />,
                    'SELLER_READ',
                )}
            />
            <Route
                path="seller-applications"
                element={withAdminPermission(
                    <PortalPage>
                        <AdminSellerApplicationPanel />
                    </PortalPage>,
                    'SELLER_APPLICATION_REVIEW',
                )}
            />
            <Route path="categories" element={withAdminPermission(
                <AdminCategoryManagementPage mode="list" />,
                'CATEGORY_READ',
            )} />
            <Route path="categories/new" element={withAdminPermission(
                <AdminCategoryManagementPage mode="new" />,
                'CATEGORY_MANAGE_ALL',
            )} />
            <Route
                path="categories/:categoryId"
                element={withAdminPermission(
                    <AdminCategoryManagementPage mode="detail" />,
                    'CATEGORY_READ',
                )}
            />
            <Route path="products" element={withAdminPermission(
                <AdminProductReviewListPage />,
                'PRODUCT_REVIEW',
            )} />
            <Route path="products/:productId" element={withAdminPermission(
                <AdminProductReviewDetailPage />,
                'PRODUCT_REVIEW',
            )} />
            <Route path="product-change-requests" element={withAdminPermission(
                <AdminProductChangeReviewListPage />,
                'PRODUCT_REVIEW',
            )} />
            <Route path="product-change-requests/:requestId" element={withAdminPermission(
                <AdminProductChangeReviewDetailPage />,
                'PRODUCT_REVIEW',
            )} />
            <Route path="orders" element={withAdminPermission(
                <AdminResourceListPage resource="orders" />,
                'REFUND_STANDARD',
            )} />
            <Route
                path="orders/:resourceId"
                element={withAdminPermission(
                    <AdminResourceDetailPage resource="orders" />,
                    'REFUND_STANDARD',
                )}
            />
            <Route path="notifications" element={<NotificationPage />} />
            <Route path="support" element={withAdminPermission(
                <SupportInquiryListPage admin />,
                'SUPPORT_REPLY',
            )} />
            <Route path="support/:inquiryId" element={withAdminPermission(
                <SupportInquiryDetailPage admin />,
                'SUPPORT_REPLY',
            )} />
            <Route
                path="settlement"
                element={withAdminPermission(
                    <PortalPage><AdminSettlementRequestList /></PortalPage>,
                    'SETTLEMENT_REVIEW',
                )}
            />
            <Route
                path="settlement/:settlementRequestId"
                element={withAdminPermission(
                    <SettlementRequestDetailPage role="admin" />,
                    'SETTLEMENT_REVIEW',
                )}
            />
            <Route path="*" element={<Navigate to="/admin" replace />} />
        </Routes>
    )
}

function withAdminPermission(element: ReactNode, ...permissions: AdminPermission[]) {
    return (
        <RequireAdminPermission permissions={permissions}>
            {element}
        </RequireAdminPermission>
    )
}

function SellerProductEditorRoute() {
    const { productId } = useParams()
    const parsedProductId = Number(productId)

    if (!Number.isInteger(parsedProductId) || parsedProductId <= 0) {
        return <Navigate to="/seller/products" replace />
    }

    return (
        <SellerProductEditorPage initialProductId={parsedProductId} />
    )
}

function StoreRoutes() {
    return (
        <Routes>
            <Route path="/" element={<ProductListPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/password-reset" element={<PasswordResetPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />
            <Route path="/oauth2/signup" element={<OAuthSignupPage />} />
            <Route path="/products/:productId" element={<ProductDetailPage />} />
            <Route path="/cart" element={<RequireAuth><CartPage /></RequireAuth>} />
            <Route
                path="/checkout"
                element={<RequireAuth><CheckoutPage /></RequireAuth>}
            />
            <Route path="/orders" element={<Navigate to="/mypage/orders" replace />} />
            <Route path="/notifications" element={<Navigate to="/mypage/notifications" replace />} />
            <Route path="/forbidden" element={<RequireAuth><AccessDeniedPage /></RequireAuth>} />
            <Route path="/orders/:orderId/payment" element={<RequireAuth><PaymentPage /></RequireAuth>} />
            <Route path="/orders/:orderId/payment/success" element={<RequireAuth><TossPaymentSuccessPage /></RequireAuth>} />
            <Route path="/orders/:orderId/payment/fail" element={<RequireAuth><TossPaymentFailPage /></RequireAuth>} />
            <Route path="/orders/:orderId/result" element={<RequireAuth><OrderResultPage /></RequireAuth>} />
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    )
}

function PortalPage({ children }: { children: ReactNode }) {
    return (
        <div className="mx-auto max-w-350 px-4 py-10 min-[601px]:px-8 min-[601px]:py-14">
            {children}
        </div>
    )
}

export default App
