import type { ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAuth } from './auth/RequireAuth'
import { RequireRole } from './auth/RequireRole'
import { Layout } from './components/Layout'
import { ManagementLayout } from './components/management/ManagementLayout'
import { AdminSellerApplicationPanel } from './components/admin/AdminSellerApplicationPanel'
import { SellerApplicationPanel } from './components/member/SellerApplicationPanel'
import { CartPage } from './pages/CartPage'
import { CheckoutPage } from './pages/CheckoutPage'
import { LoginPage } from './pages/LoginPage'
import { MyPage } from './pages/MyPage'
import { NotificationPage } from './pages/NotificationPage'
import { OrderHistoryPage } from './pages/OrderHistoryPage'
import { OrderResultPage } from './pages/OrderResultPage'
import { PaymentPage } from './pages/PaymentPage'
import { PasswordResetPage } from './pages/PasswordResetPage'
import { ProductDetailPage } from './pages/ProductDetailPage'
import { ProductListPage } from './pages/ProductListPage'
import { SellerManagementPage } from './pages/SellerManagementPage'
import { SignupPage } from './pages/SignupPage'
import { TossPaymentFailPage } from './pages/TossPaymentFailPage'
import { TossPaymentSuccessPage } from './pages/TossPaymentSuccessPage'
import { OAuth2CallbackPage } from './pages/OAuth2CallbackPage'
import { OAuthSignupPage } from './pages/OAuthSignupPage'
import { AdminManagementPage } from './pages/AdminManagementPage'
import { AdminCategoryManagementPage } from './pages/AdminCategoryManagementPage'
import { AccessDeniedPage } from './pages/AccessDeniedPage'

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
            <Route path="orders" element={<OrderHistoryPage />} />
            <Route path="notifications" element={<NotificationPage />} />
            <Route
                path="seller-application"
                element={
                    <PortalPage>
                        <SellerApplicationPanel />
                    </PortalPage>
                }
            />
            <Route path="*" element={<Navigate to="/mypage" replace />} />
        </Routes>
    )
}

function SellerPortalRoutes() {
    return (
        <Routes>
            <Route index element={<SellerManagementPage />} />
            <Route path="*" element={<Navigate to="/seller" replace />} />
        </Routes>
    )
}

function AdminPortalRoutes() {
    return (
        <Routes>
            <Route index element={<AdminManagementPage />} />
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
            <Route path="*" element={<Navigate to="/admin" replace />} />
        </Routes>
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
