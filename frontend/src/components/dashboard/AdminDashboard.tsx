import { LoaderCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { getAdminDashboardStatistics } from '../../api/dashboard'
import type { AdminDashboardStatistics, DashboardPeriodCode } from '../../types/dashboard'
import { formatPrice } from '../../utils/product'
import { REALTIME_EVENT } from '../../realtime/RealtimeProvider'
import {
    DashboardEmpty,
    DashboardPanel,
    MetricCard,
    PeriodSelector,
    RankedBars,
    SalesTrendChart,
} from './DashboardPrimitives'
import { useAdminAuthorization } from '../../auth/useAdminAuthorization'

export function AdminDashboard() {
    const { hasPermission } = useAdminAuthorization()
    const [period, setPeriod] = useState<DashboardPeriodCode>('30d')
    const [statistics, setStatistics] = useState<AdminDashboardStatistics | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const [errorMessage, setErrorMessage] = useState('')
    const [refreshVersion, setRefreshVersion] = useState(0)

    useEffect(() => {
        const refresh = (event: Event) => {
            const detail = (event as CustomEvent<{ type?: string }>).detail
            if (!detail?.type || detail.type === 'DASHBOARD_INVALIDATED'
                || detail.type.startsWith('SUPPORT_')) {
                setRefreshVersion((version) => version + 1)
            }
        }
        window.addEventListener(REALTIME_EVENT, refresh)
        window.addEventListener('focus', refresh)
        return () => {
            window.removeEventListener(REALTIME_EVENT, refresh)
            window.removeEventListener('focus', refresh)
        }
    }, [])

    useEffect(() => {
        const controller = new AbortController()
        getAdminDashboardStatistics(period, controller.signal)
            .then(setStatistics)
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError
                    ? error.message
                    : '운영 통계를 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [period, refreshVersion])

    if (isLoading && !statistics) {
        return <div className="grid min-h-80 place-content-center"><LoaderCircle className="size-6 animate-spin" /></div>
    }
    if (!statistics) {
        return <DashboardEmpty>{errorMessage || '표시할 통계가 없습니다.'}</DashboardEmpty>
    }

    const pendingTotal = Object.values(statistics.pendingTasks).reduce((sum, value) => sum + value, 0)
    const pendingQueues = [
        hasPermission('PRODUCT_REVIEW')
            ? { href: '/admin/products', label: '상품 승인', value: statistics.pendingTasks.products }
            : null,
        hasPermission('SELLER_APPLICATION_DECIDE')
            ? { href: '/admin/seller-applications', label: '판매자 승인', value: statistics.pendingTasks.sellers }
            : null,
        hasPermission('REFUND_STANDARD')
            ? { href: '/admin/orders?workType=PENDING_REFUND', label: '환불 처리', value: statistics.pendingTasks.refunds }
            : null,
        hasPermission('REFUND_STANDARD')
            ? { href: '/admin/orders?workType=PENDING_RETURN', label: '반품 처리', value: statistics.pendingTasks.returns }
            : null,
        hasPermission('SETTLEMENT_APPROVE')
            ? { href: '/admin/settlement?workType=ACTION_REQUIRED', label: '정산 처리', value: statistics.pendingTasks.settlements }
            : null,
        hasPermission('SUPPORT_REPLY')
            ? { href: '/admin/support?status=WAITING', label: '고객센터 문의', value: statistics.pendingTasks.support }
            : null,
    ].filter((queue): queue is { href: string; label: string; value: number } =>
        queue !== null)

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
                <MetricCard eyebrow="NET TRANSACTION" value={formatPrice(statistics.netTransactionAmount)} detail="환불 완료 금액을 제외한 전체 거래액" accent />
                <MetricCard eyebrow="ORDERS" value={`${statistics.orderCount.toLocaleString('ko-KR')}건`} detail="결제가 완료된 전체 주문 이력" />
                <MetricCard eyebrow="ITEMS SOLD" value={`${statistics.salesQuantity.toLocaleString('ko-KR')}개`} detail="환불 완료 수량을 제외한 판매량" />
                <MetricCard eyebrow="ACTION REQUIRED" value={`${pendingTotal.toLocaleString('ko-KR')}건`} detail="승인 및 처리 대기 업무" />
            </div>

            <div className="grid gap-3 min-[1200px]:min-h-0 min-[1200px]:flex-1 min-[1200px]:grid-cols-12 min-[1200px]:grid-rows-[minmax(180px,0.85fr)_minmax(240px,1.15fr)]">
                <DashboardPanel className="min-[1200px]:col-span-8 min-[1200px]:h-full" eyebrow="TRANSACTION FLOW" title="전체 거래 추이" aside={<span className="text-[10px] text-muted">KST</span>}>
                    <SalesTrendChart points={statistics.transactionTrend} variant="bars" legend="거래액" />
                </DashboardPanel>
                <DashboardPanel className="min-[1200px]:col-span-4 min-[1200px]:h-full" eyebrow="REGISTRATION" title="신규 회원·판매자">
                    <RegistrationChart points={statistics.registrationTrend} />
                </DashboardPanel>
                <DashboardPanel className="min-[1200px]:col-span-4 min-[1200px]:h-full" eyebrow="CATEGORY SALES" title="카테고리별 거래액">
                    <RankedBars
                        items={statistics.categorySales.slice(0, 8).map((category) => ({
                            id: category.categoryId,
                            label: category.categoryName,
                            value: category.netSalesAmount,
                            detail: `${category.salesQuantity.toLocaleString('ko-KR')}개 판매`,
                        }))}
                        compact
                        emptyMessage="기간 내 카테고리 매출이 없습니다."
                        emptySlotLabel="카테고리 없음"
                        slotCount={8}
                        valueFormatter={formatPrice}
                    />
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
                <DashboardPanel className="min-[1200px]:col-span-4 min-[1200px]:h-full" eyebrow="OPERATIONS QUEUE" title="처리 대기 업무">
                    <div className="grid h-full auto-rows-fr gap-px bg-line min-[501px]:grid-cols-2">
                        {pendingQueues.map((queue) => (
                            <PendingQueue key={queue.href} {...queue} />
                        ))}
                    </div>
                </DashboardPanel>
            </div>
        </div>
    )
}

function RegistrationChart({
    points,
}: {
    points: AdminDashboardStatistics['registrationTrend']
}) {
    const [hoveredIndex, setHoveredIndex] = useState<number | null>(null)
    const max = Math.max(...points.flatMap((point) => [point.members, point.sellers]), 1)
    if (points.length === 0) return <DashboardEmpty>기간 내 가입 데이터가 없습니다.</DashboardEmpty>

    return (
        <div className="flex h-full min-h-0 flex-col">
            <div className="mb-2 flex gap-5 text-[10px] text-muted">
                <span className="flex items-center gap-2"><i className="size-2 bg-accent" />회원</span>
                <span className="flex items-center gap-2"><i className="size-2 bg-[#272b1d] dark:bg-[#e7e8df]" />판매자</span>
            </div>
            <div className="relative flex min-h-16 flex-1 items-end gap-1 border-b border-line px-1">
                {points.map((point, index) => (
                    <div
                        className="flex h-full min-w-0 flex-1 cursor-help items-end justify-center gap-px"
                        key={point.date}
                        tabIndex={0}
                        aria-label={registrationTooltipText(point)}
                        onFocus={() => setHoveredIndex(index)}
                        onBlur={() => setHoveredIndex(null)}
                        onPointerEnter={() => setHoveredIndex(index)}
                        onPointerLeave={() => setHoveredIndex(null)}
                    >
                            <div
                                className="w-[32%] min-w-1 bg-accent"
                                style={{ height: `${Math.max(point.members / max * 100, point.members > 0 ? 4 : 0)}%` }}
                                title={`회원 ${point.members}명`}
                            />
                            <div
                                className="w-[32%] min-w-1 bg-[#272b1d] dark:bg-[#e7e8df]"
                                style={{ height: `${Math.max(point.sellers / max * 100, point.sellers > 0 ? 4 : 0)}%` }}
                                title={`판매자 ${point.sellers}명`}
                            />
                    </div>
                ))}
                {hoveredIndex !== null && points[hoveredIndex] && (
                    <div
                        className="pointer-events-none absolute z-10 whitespace-nowrap border border-white/45 bg-[#11130f] px-2 py-1 text-[10px] font-bold text-white shadow-lg"
                        role="tooltip"
                        style={{
                            left: `${Math.min(Math.max((hoveredIndex + 0.5) / points.length * 100, 18), 82)}%`,
                            top: '28%',
                            transform: 'translate(-50%, -110%)',
                        }}
                    >
                        {registrationTooltipText(points[hoveredIndex])}
                    </div>
                )}
            </div>
            <div className="mt-1 flex justify-between text-[9px] text-muted min-[601px]:text-[10px]">
                {registrationLabelIndexes(points.length).map((index) => (
                    <span key={points[index].date}>{formatRegistrationDate(points[index].date)}</span>
                ))}
            </div>
        </div>
    )
}

function PendingQueue({ href, label, value, wide = false }: { href: string; label: string; value: number; wide?: boolean }) {
    return (
        <Link
            className={`group flex items-center justify-between bg-surface px-3 py-1 transition-shadow hover:ring-1 hover:ring-inset hover:ring-[#8ba127] focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-[#8ba127] ${wide ? 'min-[501px]:col-span-2' : ''}`}
            to={href}
            aria-label={`${label} ${value.toLocaleString('ko-KR')}건 관리 페이지로 이동`}
        >
            <span className="text-xs text-muted transition-colors group-hover:text-accent">{label}</span>
            <strong className={value > 0 ? 'text-lg text-accent' : 'text-lg'}>
                {value.toLocaleString('ko-KR')}
            </strong>
        </Link>
    )
}

function formatRegistrationDate(value: string) {
    const [, month, day] = value.split('-')
    return `${Number(month)}.${Number(day)}`
}

function registrationTooltipText(point: AdminDashboardStatistics['registrationTrend'][number]) {
    return `${formatRegistrationDate(point.date)} · 회원 ${point.members.toLocaleString('ko-KR')}명 · 판매자 ${point.sellers.toLocaleString('ko-KR')}명`
}

function registrationLabelIndexes(length: number) {
    if (length === 0) return []
    if (length <= 4) return Array.from({ length }, (_, index) => index)
    return [...new Set([0, Math.floor((length - 1) / 3), Math.floor((length - 1) * 2 / 3), length - 1])]
}
