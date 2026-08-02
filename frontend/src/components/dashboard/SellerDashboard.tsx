import { LoaderCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getSellerDashboardStatistics } from '../../api/dashboard'
import { ApiError } from '../../api/client'
import type { DashboardPeriodCode, SellerDashboardStatistics } from '../../types/dashboard'
import { formatPrice } from '../../utils/product'
import {
    DashboardEmpty,
    DashboardPanel,
    MetricCard,
    PeriodSelector,
    RankedBars,
    SalesTrendChart,
} from './DashboardPrimitives'

const orderStatusLabels: Record<string, string> = {
    PENDING_PAYMENT: '결제 대기',
    PAID: '결제 완료',
    PREPARING: '상품 준비',
    SHIPPED: '배송 중',
    DELIVERED: '배송 완료',
    CANCELED: '주문 취소',
    PAYMENT_FAILED: '결제 실패',
    PARTIALLY_REFUNDED: '부분 환불',
    REFUNDED: '환불 완료',
}

export function SellerDashboard() {
    const [period, setPeriod] = useState<DashboardPeriodCode>('30d')
    const [statistics, setStatistics] = useState<SellerDashboardStatistics | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        getSellerDashboardStatistics(period, controller.signal)
            .then(setStatistics)
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError
                    ? error.message
                    : '판매 통계를 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [period])

    if (isLoading && !statistics) {
        return <div className="grid min-h-80 place-content-center"><LoaderCircle className="size-6 animate-spin" /></div>
    }
    if (!statistics) {
        return <DashboardEmpty>{errorMessage || '표시할 통계가 없습니다.'}</DashboardEmpty>
    }

    const pendingTotal = Object.values(statistics.pendingTasks).reduce((sum, value) => sum + value, 0)
    const activeStatuses = statistics.orderStatusCounts.filter((item) => item.count > 0)

    function changePeriod(value: DashboardPeriodCode) {
        setIsLoading(true)
        setErrorMessage('')
        setPeriod(value)
    }

    return (
        <div className="flex flex-col gap-2 min-[1200px]:h-[calc(100dvh-112px)]" aria-busy={isLoading}>
            <div className="flex items-center justify-between gap-4">
                <h1 className="font-serif text-[clamp(30px,3vw,38px)] leading-none tracking-tighter">대시보드</h1>
                <PeriodSelector value={period} onChange={changePeriod} />
            </div>
            {errorMessage && <p className="border border-danger/35 bg-danger-soft p-3 text-sm text-danger" role="alert">{errorMessage}</p>}

            <div className="grid gap-3 min-[601px]:grid-cols-2 min-[1101px]:grid-cols-4">
                <MetricCard eyebrow="NET SALES" value={formatPrice(statistics.netSalesAmount)} detail="환불 완료 금액을 제외한 순매출" accent />
                <MetricCard eyebrow="ORDERS" value={`${statistics.orderCount.toLocaleString('ko-KR')}건`} detail="결제가 완료된 주문 이력" />
                <MetricCard eyebrow="ITEMS SOLD" value={`${statistics.salesQuantity.toLocaleString('ko-KR')}개`} detail="환불 완료 수량을 제외한 판매량" />
                <MetricCard eyebrow="TO DO" value={`${pendingTotal.toLocaleString('ko-KR')}건`} detail="주문·반품·문의 미처리 업무" />
            </div>

            <div className="grid gap-3 min-[1200px]:min-h-0 min-[1200px]:flex-1 min-[1200px]:grid-cols-12 min-[1200px]:grid-rows-[minmax(180px,0.85fr)_minmax(240px,1.15fr)]">
                <DashboardPanel className="min-[1200px]:col-span-8 min-[1200px]:h-full" eyebrow="SALES FLOW" title="순매출 추이" aside={<span className="text-[10px] text-muted">KST</span>}>
                    <SalesTrendChart points={statistics.trend} />
                </DashboardPanel>
                <DashboardPanel className="min-[1200px]:col-span-4 min-[1200px]:h-full" eyebrow="ACTION REQUIRED" title="처리할 업무">
                    <div className="grid h-full grid-cols-3 items-center divide-x divide-line text-center">
                        <PendingValue href="/seller/orders?workType=ACTION_REQUIRED" label="주문" value={statistics.pendingTasks.orders} />
                        <PendingValue href="/seller/returns?status=REQUESTED" label="반품" value={statistics.pendingTasks.returns} />
                        <PendingValue href="/seller/questions?status=WAITING" label="문의" value={statistics.pendingTasks.questions} />
                    </div>
                </DashboardPanel>
                <DashboardPanel className="min-[1200px]:col-span-4 min-[1200px]:h-full" eyebrow="TOP PRODUCTS" title="판매량 상위 상품">
                    <RankedBars
                        items={statistics.topProducts.slice(0, 5).map((product) => ({
                            id: product.productId,
                            label: product.productName,
                            value: product.salesQuantity,
                            detail: formatPrice(product.netSalesAmount),
                        }))}
                        emptyValue="0개"
                        emptyMessage="기간 내 판매된 상품이 없습니다."
                        emptySlotLabel="판매 데이터 없음"
                        slotCount={5}
                        valueFormatter={(value) => `${value.toLocaleString('ko-KR')}개`}
                    />
                </DashboardPanel>
                <DashboardPanel className="min-[1200px]:col-span-4 min-[1200px]:h-full" eyebrow="ORDER STATUS" title="주문 상태">
                    <OrderStatusGrid items={activeStatuses} />
                </DashboardPanel>
                <DashboardPanel className="min-[1200px]:col-span-4 min-[1200px]:h-full" eyebrow="SETTLEMENT" title="정산 현황">
                    <div className="grid h-full grid-rows-3 gap-1">
                        <SettlementValue href="/seller/settlement?tab=request" label="정산 가능" value={statistics.settlement.availableAmount} emphasis />
                        <SettlementValue href="/seller/settlement?tab=history&workType=PROCESSING" label="처리 중" value={statistics.settlement.processingAmount} />
                        <SettlementValue href="/seller/settlement?tab=history&status=PAID" label="정산 완료" value={statistics.settlement.completedAmount} />
                    </div>
                </DashboardPanel>
            </div>
        </div>
    )
}

function SettlementValue({ href, label, value, emphasis = false }: { href: string; label: string; value: number; emphasis?: boolean }) {
    return (
        <Link
            className={`${emphasis ? 'bg-success-soft' : 'border border-line'} group flex items-center justify-between gap-3 px-3 py-2 transition-shadow hover:ring-1 hover:ring-inset hover:ring-accent focus-visible:outline-2 focus-visible:outline-accent`}
            to={href}
            aria-label={`${label} ${formatPrice(value)} 정산 관리 페이지로 이동`}
        >
            <p className="text-[11px] text-muted transition-colors group-hover:text-accent">{label}</p>
            <strong className="text-sm">{formatPrice(value)}</strong>
        </Link>
    )
}

function PendingValue({ href, label, value }: { href: string; label: string; value: number }) {
    return (
        <Link
            className="group px-2 py-3 transition-shadow hover:ring-1 hover:ring-inset hover:ring-[#8ba127] focus-visible:outline-2 focus-visible:outline-[#8ba127]"
            to={href}
            aria-label={`${label} ${value.toLocaleString('ko-KR')}건 관리 페이지로 이동`}
        >
            <strong className="block text-2xl">{value}</strong>
            <span className="mt-1 block text-[11px] text-muted transition-colors group-hover:text-accent">{label}</span>
        </Link>
    )
}

function OrderStatusGrid({ items }: { items: SellerDashboardStatistics['orderStatusCounts'] }) {
    const max = Math.max(...items.map((item) => item.count), 1)
    if (items.length === 0) {
        return <DashboardEmpty><strong className="mb-1 block text-2xl text-ink dark:text-white">0건</strong>기간 내 주문이 없습니다.</DashboardEmpty>
    }

    return (
        <ol className="grid h-full auto-rows-fr grid-cols-2 gap-1 min-[1500px]:grid-cols-3">
            {items.map((item) => (
                <li className="grid min-w-0 grid-cols-[1fr_auto] content-center items-center gap-x-2 border border-line bg-surface px-2 py-1" key={item.status}>
                    <span className="truncate text-[10px] text-muted">{orderStatusLabels[item.status] ?? item.status}</span>
                    <strong className="text-sm tabular-nums">{item.count.toLocaleString('ko-KR')}건</strong>
                    <div className="col-span-2 mt-1 h-1 bg-subtle">
                        <div className="h-full bg-accent" style={{ width: `${item.count / max * 100}%` }} />
                    </div>
                </li>
            ))}
        </ol>
    )
}
