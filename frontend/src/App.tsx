import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAuth } from './auth/RequireAuth'
import { RequireRole } from './auth/RequireRole'
import { Layout } from './components/Layout'
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
import { AccessDeniedPage } from './pages/AccessDeniedPage'

function App() {
    return (
        <Layout>
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
                <Route path="/orders" element={<RequireAuth><OrderHistoryPage /></RequireAuth>} />
                <Route path="/notifications" element={<RequireAuth><NotificationPage /></RequireAuth>} />
                <Route path="/mypage" element={<RequireAuth><MyPage /></RequireAuth>} />
                <Route path="/forbidden" element={<RequireAuth><AccessDeniedPage /></RequireAuth>} />
                <Route path="/orders/:orderId/payment" element={<RequireAuth><PaymentPage /></RequireAuth>} />
                <Route path="/orders/:orderId/payment/success" element={<RequireAuth><TossPaymentSuccessPage /></RequireAuth>} />
                <Route path="/orders/:orderId/payment/fail" element={<RequireAuth><TossPaymentFailPage /></RequireAuth>} />
                <Route path="/orders/:orderId/result" element={<RequireAuth><OrderResultPage /></RequireAuth>} />
                <Route path="/seller" element={<RequireRole roles={['ROLE_SELLER', 'ROLE_ADMIN']}><SellerManagementPage /></RequireRole>} />
                <Route path="/admin" element={<RequireRole roles={['ROLE_ADMIN']}><AdminManagementPage /></RequireRole>} />
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </Layout>
    )
}

export default App
