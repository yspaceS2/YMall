import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAuth } from '../auth/RequireAuth'
import { Layout } from '../components/Layout'
import { lazyNamed } from '../utils/lazyNamed'

const AccessDeniedPage = lazyNamed(
    () => import('../pages/AccessDeniedPage'),
    (module) => module.AccessDeniedPage,
)
const CartPage = lazyNamed(() => import('../pages/CartPage'), (module) => module.CartPage)
const CheckoutPage = lazyNamed(
    () => import('../pages/CheckoutPage'),
    (module) => module.CheckoutPage,
)
const LoginPage = lazyNamed(() => import('../pages/LoginPage'), (module) => module.LoginPage)
const OAuth2CallbackPage = lazyNamed(
    () => import('../pages/OAuth2CallbackPage'),
    (module) => module.OAuth2CallbackPage,
)
const OAuthSignupPage = lazyNamed(
    () => import('../pages/OAuthSignupPage'),
    (module) => module.OAuthSignupPage,
)
const OrderResultPage = lazyNamed(
    () => import('../pages/OrderResultPage'),
    (module) => module.OrderResultPage,
)
const PasswordResetPage = lazyNamed(
    () => import('../pages/PasswordResetPage'),
    (module) => module.PasswordResetPage,
)
const PaymentPage = lazyNamed(() => import('../pages/PaymentPage'), (module) => module.PaymentPage)
const ProductDetailPage = lazyNamed(
    () => import('../pages/ProductDetailPage'),
    (module) => module.ProductDetailPage,
)
const ProductListPage = lazyNamed(
    () => import('../pages/ProductListPage'),
    (module) => module.ProductListPage,
)
const SignupPage = lazyNamed(() => import('../pages/SignupPage'), (module) => module.SignupPage)
const TossPaymentFailPage = lazyNamed(
    () => import('../pages/TossPaymentFailPage'),
    (module) => module.TossPaymentFailPage,
)
const TossPaymentSuccessPage = lazyNamed(
    () => import('../pages/TossPaymentSuccessPage'),
    (module) => module.TossPaymentSuccessPage,
)

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
