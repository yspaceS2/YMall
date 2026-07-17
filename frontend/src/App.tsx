import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAuth } from './auth/RequireAuth'
import { RequireRole } from './auth/RequireRole'
import { Layout } from './components/Layout'
import { CartPage } from './pages/CartPage'
import { CheckoutPage } from './pages/CheckoutPage'
import { LoginPage } from './pages/LoginPage'
import { OrderHistoryPage } from './pages/OrderHistoryPage'
import { OrderResultPage } from './pages/OrderResultPage'
import { PaymentPage } from './pages/PaymentPage'
import { ProductDetailPage } from './pages/ProductDetailPage'
import { ProductListPage } from './pages/ProductListPage'
import { SellerManagementPage } from './pages/SellerManagementPage'
import { AdminManagementPage } from './pages/AdminManagementPage'

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
                <Route path="/seller" element={<RequireRole roles={['ROLE_SELLER', 'ROLE_ADMIN']}><SellerManagementPage /></RequireRole>} />
                <Route path="/admin" element={<RequireRole roles={['ROLE_ADMIN']}><AdminManagementPage /></RequireRole>} />
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </Layout>
    )
}

export default App
