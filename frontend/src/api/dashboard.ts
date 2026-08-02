import type {
    AdminDashboardStatistics,
    DashboardPeriodCode,
    SellerDashboardStatistics,
} from '../types/dashboard'
import { apiRequest } from './client'

export function getSellerDashboardStatistics(
    period: DashboardPeriodCode,
    signal?: AbortSignal,
) {
    const query = new URLSearchParams({ period })
    return apiRequest<SellerDashboardStatistics>(
        `/seller/dashboard/statistics?${query.toString()}`,
        { signal },
    )
}

export function getAdminDashboardStatistics(
    period: DashboardPeriodCode,
    signal?: AbortSignal,
) {
    const query = new URLSearchParams({ period })
    return apiRequest<AdminDashboardStatistics>(
        `/admin/dashboard/statistics?${query.toString()}`,
        { signal },
    )
}
