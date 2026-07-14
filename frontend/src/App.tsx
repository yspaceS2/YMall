import { Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ProductDetailPage } from './pages/ProductDetailPage'
import { ProductListPage } from './pages/ProductListPage'
import { LoginPage } from './pages/LoginPage'
import './App.css'

function App() {
    return (
        <Layout>
            <Routes>
                <Route path="/" element={<ProductListPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/products/:productId" element={<ProductDetailPage />} />
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </Layout>
    )
}

export default App
