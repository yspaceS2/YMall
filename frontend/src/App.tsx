import { Suspense } from 'react'
import { Route, Routes } from 'react-router-dom'
import { AdminAuthorizationProvider } from './auth/AdminAuthorizationProvider'
import { RequireAuth } from './auth/RequireAuth'
import { RequireRole } from './auth/RequireRole'
import { ManagementLayout } from './components/management/ManagementLayout'
import { RouteLoadingFallback } from './components/routing/RouteLoadingFallback'
import { AdminPortalRoutes } from './routes/AdminPortalRoutes'
import { MemberPortalRoutes } from './routes/MemberPortalRoutes'
import { SellerPortalRoutes } from './routes/SellerPortalRoutes'
import { StoreRoutes } from './routes/StoreRoutes'

function App() {
    return (
        <Suspense fallback={<RouteLoadingFallback />}>
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
                <Route path="*" element={<StoreRoutes />} />
            </Routes>
        </Suspense>
    )
}

export default App
