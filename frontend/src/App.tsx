import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { CartPage } from './pages/CartPage'
import { CheckoutPage } from './pages/CheckoutPage'
import { LoginPage } from './pages/LoginPage'
import { OrderHistoryPage } from './pages/OrderHistoryPage'
import { OrderResultPage } from './pages/OrderResultPage'
import { PaymentPage } from './pages/PaymentPage'
import { ProductDetailPage } from './pages/ProductDetailPage'
import { ProductListPage } from './pages/ProductListPage'

function App() {
    return (
        <Layout>
            <Routes>
                <Route path="/" element={<ProductListPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/products/:productId" element={<ProductDetailPage />} />
                <Route path="/cart" element={<RequireAuth><CartPage /></RequireAuth>} />
                <Route
                    path="/checkout"
                    element={<RequireAuth><CheckoutPage /></RequireAuth>}
                />
                <Route path="/orders" element={<RequireAuth><OrderHistoryPage /></RequireAuth>} />
                <Route path="/orders/:orderId/payment" element={<RequireAuth><PaymentPage /></RequireAuth>} />
                <Route path="/orders/:orderId/result" element={<RequireAuth><OrderResultPage /></RequireAuth>} />
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </Layout>
    )
}

export default App
