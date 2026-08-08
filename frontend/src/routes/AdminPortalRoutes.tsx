import type { ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAdminPermission } from '../auth/RequireAdminPermission'
import type { AdminPermission } from '../types/admin'
import { lazyNamed } from '../utils/lazyNamed'
import { PortalPage } from './PortalPage'
import { ADMIN_ROUTE_PERMISSIONS } from './adminRoutePermissions'

const AdminSellerApplicationPanel = lazyNamed(
    () => import('../components/admin/AdminSellerApplicationPanel'),
    (module) => module.AdminSellerApplicationPanel,
)
const AdminSettlementRequestList = lazyNamed(
    () => import('../components/settlement/SettlementRequestList'),
    (module) => module.AdminSettlementRequestList,
)
const AdminCategoryManagementPage = lazyNamed(
    () => import('../pages/AdminCategoryManagementPage'),
    (module) => module.AdminCategoryManagementPage,
)
const AdminManagementPage = lazyNamed(
    () => import('../pages/AdminManagementPage'),
    (module) => module.AdminManagementPage,
)
const AdminProductChangeReviewDetailPage = lazyNamed(
    () => import('../pages/AdminProductChangeReviewPage'),
    (module) => module.AdminProductChangeReviewDetailPage,
)
const AdminProductChangeReviewListPage = lazyNamed(
    () => import('../pages/AdminProductChangeReviewPage'),
    (module) => module.AdminProductChangeReviewListPage,
)
const AdminProductReviewDetailPage = lazyNamed(
    () => import('../pages/AdminProductReviewPage'),
    (module) => module.AdminProductReviewDetailPage,
)
const AdminProductReviewListPage = lazyNamed(
    () => import('../pages/AdminProductReviewPage'),
    (module) => module.AdminProductReviewListPage,
)
const AdminResourceDetailPage = lazyNamed(
    () => import('../pages/AdminResourceDetailPage'),
    (module) => module.AdminResourceDetailPage,
)
const AdminResourceListPage = lazyNamed(
    () => import('../pages/AdminResourceListPage'),
    (module) => module.AdminResourceListPage,
)
const NotificationPage = lazyNamed(
    () => import('../pages/NotificationPage'),
    (module) => module.NotificationPage,
)
const SettlementRequestDetailPage = lazyNamed(
    () => import('../pages/SettlementRequestDetailPage'),
    (module) => module.SettlementRequestDetailPage,
)
const SupportInquiryDetailPage = lazyNamed(
    () => import('../pages/SupportInquiryDetailPage'),
    (module) => module.SupportInquiryDetailPage,
)
const SupportInquiryListPage = lazyNamed(
    () => import('../pages/SupportInquiryListPage'),
    (module) => module.SupportInquiryListPage,
)

export function AdminPortalRoutes() {
    return (
        <Routes>
            <Route index element={withAdminPermission(
                <AdminManagementPage />,
                ADMIN_ROUTE_PERMISSIONS.dashboard,
            )} />
            <Route path="members" element={withAdminPermission(
                <AdminResourceListPage resource="members" />,
                ADMIN_ROUTE_PERMISSIONS.members,
            )} />
            <Route
                path="members/:resourceId"
                element={withAdminPermission(
                    <AdminResourceDetailPage resource="members" />,
                    ADMIN_ROUTE_PERMISSIONS.members,
                )}
            />
            <Route path="sellers" element={withAdminPermission(
                <AdminResourceListPage resource="sellers" />,
                ADMIN_ROUTE_PERMISSIONS.sellers,
            )} />
            <Route
                path="sellers/:resourceId"
                element={withAdminPermission(
                    <AdminResourceDetailPage resource="sellers" />,
                    ADMIN_ROUTE_PERMISSIONS.sellers,
                )}
            />
            <Route
                path="seller-applications"
                element={withAdminPermission(
                    <PortalPage><AdminSellerApplicationPanel /></PortalPage>,
                    ADMIN_ROUTE_PERMISSIONS.sellerApplications,
                )}
            />
            <Route path="categories" element={withAdminPermission(
                <AdminCategoryManagementPage mode="list" />,
                ADMIN_ROUTE_PERMISSIONS.categoryRead,
            )} />
            <Route path="categories/new" element={withAdminPermission(
                <AdminCategoryManagementPage mode="new" />,
                ADMIN_ROUTE_PERMISSIONS.categoryManage,
            )} />
            <Route
                path="categories/:categoryId"
                element={withAdminPermission(
                    <AdminCategoryManagementPage mode="detail" />,
                    ADMIN_ROUTE_PERMISSIONS.categoryRead,
                )}
            />
            <Route path="products" element={withAdminPermission(
                <AdminProductReviewListPage />,
                ADMIN_ROUTE_PERMISSIONS.productReview,
            )} />
            <Route path="products/:productId" element={withAdminPermission(
                <AdminProductReviewDetailPage />,
                ADMIN_ROUTE_PERMISSIONS.productReview,
            )} />
            <Route path="product-change-requests" element={withAdminPermission(
                <AdminProductChangeReviewListPage />,
                ADMIN_ROUTE_PERMISSIONS.productReview,
            )} />
            <Route path="product-change-requests/:requestId" element={withAdminPermission(
                <AdminProductChangeReviewDetailPage />,
                ADMIN_ROUTE_PERMISSIONS.productReview,
            )} />
            <Route path="orders" element={withAdminPermission(
                <AdminResourceListPage resource="orders" />,
                ADMIN_ROUTE_PERMISSIONS.orders,
            )} />
            <Route
                path="orders/:resourceId"
                element={withAdminPermission(
                    <AdminResourceDetailPage resource="orders" />,
                    ADMIN_ROUTE_PERMISSIONS.orders,
                )}
            />
            <Route path="notifications" element={<NotificationPage />} />
            <Route path="support" element={withAdminPermission(
                <SupportInquiryListPage admin />,
                ADMIN_ROUTE_PERMISSIONS.support,
            )} />
            <Route path="support/:inquiryId" element={withAdminPermission(
                <SupportInquiryDetailPage admin />,
                ADMIN_ROUTE_PERMISSIONS.support,
            )} />
            <Route
                path="settlement"
                element={withAdminPermission(
                    <PortalPage><AdminSettlementRequestList /></PortalPage>,
                    ADMIN_ROUTE_PERMISSIONS.settlement,
                )}
            />
            <Route
                path="settlement/:settlementRequestId"
                element={withAdminPermission(
                    <SettlementRequestDetailPage role="admin" />,
                    ADMIN_ROUTE_PERMISSIONS.settlement,
                )}
            />
            <Route path="*" element={<Navigate to="/admin" replace />} />
        </Routes>
    )
}

function withAdminPermission(
    element: ReactNode,
    permissions: readonly AdminPermission[],
) {
    return (
        <RequireAdminPermission permissions={[...permissions]}>
            {element}
        </RequireAdminPermission>
    )
}
