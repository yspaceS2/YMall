import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { CartPage } from './pages/CartPage'
import { CheckoutPlaceholderPage } from './pages/CheckoutPlaceholderPage'
import { LoginPage } from './pages/LoginPage'
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
                    element={<RequireAuth><CheckoutPlaceholderPage /></RequireAuth>}
                />
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </Layout>
    )
}

export default App
