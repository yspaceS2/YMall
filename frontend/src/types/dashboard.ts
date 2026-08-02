export type DashboardPeriodCode = '7d' | '30d' | '6m' | '1y'

export interface DashboardPeriod {
    period: DashboardPeriodCode
    from: string
    to: string
    interval: 'DAY' | 'MONTH'
}

export interface DashboardTrendPoint {
    date: string
    netSalesAmount: number
    orderCount: number
    salesQuantity: number
}

export interface DashboardTopProduct {
    productId: number
    productName: string
    salesQuantity: number
    netSalesAmount: number
}

export interface SellerDashboardStatistics {
    period: DashboardPeriod
    netSalesAmount: number
    orderCount: number
    salesQuantity: number
    trend: DashboardTrendPoint[]
    orderStatusCounts: Array<{
        status: string
        count: number
    }>
    topProducts: DashboardTopProduct[]
    settlement: {
        availableAmount: number
        processingAmount: number
        completedAmount: number
    }
    pendingTasks: {
        orders: number
        returns: number
        questions: number
    }
    generatedAt: string
}

export interface AdminDashboardStatistics {
    period: DashboardPeriod
    netTransactionAmount: number
    orderCount: number
    salesQuantity: number
    transactionTrend: DashboardTrendPoint[]
    registrationTrend: Array<{
        date: string
        members: number
        sellers: number
    }>
    categorySales: Array<{
        categoryId: number
        categoryName: string
        netSalesAmount: number
        salesQuantity: number
    }>
    topProducts: DashboardTopProduct[]
    pendingTasks: {
        products: number
        sellers: number
        refunds: number
        returns: number
        settlements: number
    }
    generatedAt: string
}
