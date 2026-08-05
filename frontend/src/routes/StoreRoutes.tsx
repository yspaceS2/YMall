import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAuth } from '../auth/RequireAuth'
import { Layout } from '../components/Layout'
import { AccessDeniedPage } from '../pages/AccessDeniedPage'
import { CartPage } from '../pages/CartPage'
import { CheckoutPage } from '../pages/CheckoutPage'
import { LoginPage } from '../pages/LoginPage'
import { OAuth2CallbackPage } from '../pages/OAuth2CallbackPage'
import { OAuthSignupPage } from '../pages/OAuthSignupPage'
import { OrderResultPage } from '../pages/OrderResultPage'
import { PasswordResetPage } from '../pages/PasswordResetPage'
import { PaymentPage } from '../pages/PaymentPage'
import { ProductDetailPage } from '../pages/ProductDetailPage'
import { ProductListPage } from '../pages/ProductListPage'
import { SignupPage } from '../pages/SignupPage'
import { TossPaymentFailPage } from '../pages/TossPaymentFailPage'
import { TossPaymentSuccessPage } from '../pages/TossPaymentSuccessPage'

export function StoreRoutes() {
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
                <Route path="/orders" element={<Navigate to="/mypage/orders" replace />} />
                <Route path="/notifications" element={<Navigate to="/mypage/notifications" replace />} />
                <Route path="/forbidden" element={<RequireAuth><AccessDeniedPage /></RequireAuth>} />
                <Route path="/orders/:orderId/payment" element={<RequireAuth><PaymentPage /></RequireAuth>} />
                <Route path="/orders/:orderId/payment/success" element={<RequireAuth><TossPaymentSuccessPage /></RequireAuth>} />
                <Route path="/orders/:orderId/payment/fail" element={<RequireAuth><TossPaymentFailPage /></RequireAuth>} />
                <Route path="/orders/:orderId/result" element={<RequireAuth><OrderResultPage /></RequireAuth>} />
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </Layout>
    )
}
