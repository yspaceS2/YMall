import type { ReactNode } from 'react'
import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import { RequireAuth } from './auth/RequireAuth'
import { RequireRole } from './auth/RequireRole'
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
import { SellerManagementPage } from './pages/SellerManagementPage'
import {
    SellerOrderDetailPage,
    SellerOrderListPage,
} from './pages/SellerOrderManagementPage'
import { SellerProductListPage } from './pages/SellerResourcePages'
import { SignupPage } from './pages/SignupPage'
import { TossPaymentFailPage } from './pages/TossPaymentFailPage'
import { TossPaymentSuccessPage } from './pages/TossPaymentSuccessPage'
import { OAuth2CallbackPage } from './pages/OAuth2CallbackPage'
import { OAuthSignupPage } from './pages/OAuthSignupPage'
import { AdminManagementPage } from './pages/AdminManagementPage'
import {
    AdminResourceDetailPage,
    AdminResourceListPage,
} from './pages/AdminResourcePages'
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
                        <ManagementLayout role="admin">
                            <AdminPortalRoutes />
                        </ManagementLayout>
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
            <Route index element={<SellerManagementPage section="dashboard" />} />
            <Route path="profile" element={<SellerManagementPage section="profile" />} />
            <Route path="products" element={<SellerProductListPage />} />
            <Route
                path="products/new"
                element={<SellerManagementPage section="products" productFormOnly />}
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
            <Route index element={<AdminManagementPage section="dashboard" />} />
            <Route path="members" element={<AdminResourceListPage resource="members" />} />
            <Route
                path="members/:resourceId"
                element={<AdminResourceDetailPage resource="members" />}
            />
            <Route path="sellers" element={<AdminResourceListPage resource="sellers" />} />
            <Route
                path="sellers/:resourceId"
                element={<AdminResourceDetailPage resource="sellers" />}
            />
            <Route
                path="seller-applications"
                element={
                    <PortalPage>
                        <AdminSellerApplicationPanel />
                    </PortalPage>
                }
            />
            <Route path="categories" element={<AdminCategoryManagementPage mode="list" />} />
            <Route path="categories/new" element={<AdminCategoryManagementPage mode="new" />} />
            <Route
                path="categories/:categoryId"
                element={<AdminCategoryManagementPage mode="detail" />}
            />
            <Route path="products" element={<AdminProductReviewListPage />} />
            <Route path="products/:productId" element={<AdminProductReviewDetailPage />} />
            <Route path="product-change-requests" element={<AdminProductChangeReviewListPage />} />
            <Route path="product-change-requests/:requestId" element={<AdminProductChangeReviewDetailPage />} />
            <Route path="orders" element={<AdminResourceListPage resource="orders" />} />
            <Route
                path="orders/:resourceId"
                element={<AdminResourceDetailPage resource="orders" />}
            />
            <Route path="notifications" element={<NotificationPage />} />
            <Route path="support" element={<SupportInquiryListPage admin />} />
            <Route path="support/:inquiryId" element={<SupportInquiryDetailPage admin />} />
            <Route
                path="settlement"
                element={<PortalPage><AdminSettlementRequestList /></PortalPage>}
            />
            <Route
                path="settlement/:settlementRequestId"
                element={<SettlementRequestDetailPage role="admin" />}
            />
            <Route path="*" element={<Navigate to="/admin" replace />} />
        </Routes>
    )
}

function SellerProductEditorRoute() {
    const { productId } = useParams()
    const parsedProductId = Number(productId)

    if (!Number.isInteger(parsedProductId) || parsedProductId <= 0) {
        return <Navigate to="/seller/products" replace />
    }

    return (
        <SellerManagementPage
            section="products"
            productFormOnly
            initialProductId={parsedProductId}
        />
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
