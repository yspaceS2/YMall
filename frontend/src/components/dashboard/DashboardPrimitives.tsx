import { useId, useState, type ReactNode } from 'react'
import type { DashboardPeriodCode, DashboardTrendPoint } from '../../types/dashboard'
import { formatPrice } from '../../utils/product'

const periodOptions: Array<{ value: DashboardPeriodCode; label: string }> = [
    { value: '7d', label: '7일' },
    { value: '30d', label: '30일' },
    { value: '6m', label: '6개월' },
    { value: '1y', label: '1년' },
]

export function PeriodSelector({
    value,
    onChange,
}: {
    value: DashboardPeriodCode
    onChange: (value: DashboardPeriodCode) => void
}) {
    return (
        <div className="flex flex-wrap gap-1 border border-line bg-surface p-1" aria-label="통계 기간">
            {periodOptions.map((option) => (
                <button
                    className={value === option.value
                        ? 'bg-ink px-4 py-2 text-xs font-extrabold text-white'
                        : 'px-4 py-2 text-xs font-bold text-muted transition-colors hover:text-ink dark:hover:text-white'}
                    key={option.value}
                    type="button"
                    aria-pressed={value === option.value}
                    onClick={() => onChange(option.value)}
                >
                    {option.label}
                </button>
            ))}
        </div>
    )
}

export function MetricCard({
    eyebrow,
    value,
    detail,
    accent = false,
}: {
    eyebrow: string
    value: string
    detail: string
    accent?: boolean
}) {
    const hasLongValue = value.length >= 10
    return (
        <article className={accent
            ? 'relative overflow-hidden bg-[#202516] p-4 text-white dark:bg-[#c9db72] dark:text-[#15170f]'
            : 'border border-line bg-surface p-3'}
        >
            {accent && <span className="absolute -right-8 -top-8 size-24 rounded-full border-16 border-white/8 dark:border-black/6" />}
            <p className={accent
                ? 'relative text-[10px] font-extrabold tracking-[.16em] text-white/60 dark:text-black/55'
                : 'text-[10px] font-extrabold tracking-[.16em] text-muted'}
            >
                {eyebrow}
            </p>
            <strong className={`relative mt-1.5 block whitespace-nowrap leading-none tracking-tight ${
                hasLongValue ? 'text-[clamp(21px,1.8vw,27px)]' : 'text-[clamp(24px,3vw,36px)]'
            }`}>
                {value}
            </strong>
            <p className={accent
                ? 'relative mt-2 text-[11px] text-white/65 dark:text-black/60'
                : 'mt-1.5 text-[10px] text-muted'}
            >
                {detail}
            </p>
        </article>
    )
}

export function DashboardPanel({
    eyebrow,
    title,
    aside,
    children,
    className = '',
}: {
    eyebrow: string
    title: string
    aside?: ReactNode
    children: ReactNode
    className?: string
}) {
    return (
        <section className={`flex min-w-0 flex-col border border-line bg-surface p-3 ${className}`}>
            <header className="mb-2 flex flex-wrap items-end justify-between gap-3">
                <div>
                    <p className="text-[10px] font-extrabold tracking-[.16em] text-[#71801e] dark:text-[#c9db72]">
                        {eyebrow}
                    </p>
                    <h2 className="mt-0.5 text-base font-bold tracking-tight">{title}</h2>
                </div>
                {aside}
            </header>
            <div className="min-h-0 flex-1">{children}</div>
        </section>
    )
}

export function SalesTrendChart({
    points,
    variant = 'line',
    legend = '순매출',
}: {
    points: DashboardTrendPoint[]
    variant?: 'line' | 'bars'
    legend?: string
}) {
    const gradientId = useId().replaceAll(':', '')
    const [hoveredIndex, setHoveredIndex] = useState<number | null>(null)
    const width = 760
    const height = 220
    const padding = { top: 12, right: 10, bottom: 34, left: 10 }
    const plotWidth = width - padding.left - padding.right
    const plotHeight = height - padding.top - padding.bottom
    const values = points.map((point) => point.netSalesAmount)
    const maxValue = Math.max(...values, 1)
    const barStep = plotWidth / Math.max(points.length, 1)
    const barWidth = Math.min(Math.max(barStep * 0.58, 4), 22)
    const coordinates = values.map((value, index) => ({
        x: variant === 'bars'
            ? padding.left + barStep * (index + 0.5)
            : padding.left + (points.length === 1 ? plotWidth / 2 : index * plotWidth / (points.length - 1)),
        y: padding.top + plotHeight - value / maxValue * plotHeight,
    }))
    const line = coordinates.map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x},${point.y}`).join(' ')
    const area = coordinates.length === 0
        ? ''
        : `${line} L${coordinates.at(-1)?.x},${padding.top + plotHeight} L${coordinates[0].x},${padding.top + plotHeight} Z`
    const labelIndexes = chartLabelIndexes(points.length)

    return (
        <div className="flex h-full min-h-0 flex-col">
            <div className="mb-1 flex flex-wrap items-end justify-between gap-3">
                <div>
                    <p className="text-xs text-muted">기간 내 최고 일매출</p>
                    <strong className="mt-0.5 block text-lg">{formatPrice(maxValue === 1 ? 0 : maxValue)}</strong>
                </div>
                <div className="flex items-center gap-2 text-[11px] text-muted">
                    <span className={variant === 'bars' ? 'size-2 bg-[#8ba127]' : 'size-2 rounded-full bg-[#8ba127]'} /> {legend}
                </div>
            </div>
            <div className="relative min-h-20 flex-1">
                <svg
                    className="h-full w-full overflow-visible text-[#71801e] dark:text-[#c9db72]"
                    viewBox={`0 0 ${width} ${height}`}
                    role="img"
                    aria-label="기간별 순매출 추이"
                >
                <defs>
                    <linearGradient id={gradientId} x1="0" x2="0" y1="0" y2="1">
                        <stop offset="0" stopColor="currentColor" stopOpacity="0.28" />
                        <stop offset="1" stopColor="currentColor" stopOpacity="0" />
                    </linearGradient>
                </defs>
                {[0, 0.5, 1].map((ratio) => (
                    <line
                        key={ratio}
                        x1={padding.left}
                        x2={width - padding.right}
                        y1={padding.top + plotHeight * ratio}
                        y2={padding.top + plotHeight * ratio}
                        stroke="currentColor"
                        strokeOpacity="0.12"
                        strokeDasharray="4 5"
                    />
                ))}
                {variant === 'line' && area && <path d={area} fill={`url(#${gradientId})`} />}
                {variant === 'line' && line && <path d={line} fill="none" stroke="currentColor" strokeWidth="3" strokeLinejoin="round" />}
                {variant === 'line' && coordinates.map((point, index) => values[index] > 0 && (
                    <circle key={points[index].date} cx={point.x} cy={point.y} r="3.5" fill="currentColor" />
                ))}
                {variant === 'bars' && coordinates.map((point, index) => values[index] > 0 && (
                    <rect
                        key={points[index].date}
                        x={point.x - barWidth / 2}
                        y={point.y}
                        width={barWidth}
                        height={padding.top + plotHeight - point.y}
                        fill="currentColor"
                        opacity="0.88"
                    />
                ))}
                {points.map((point, index) => (
                    <rect
                        key={`hit-${point.date}`}
                        x={padding.left + barStep * index}
                        y={padding.top}
                        width={barStep}
                        height={plotHeight}
                        fill="transparent"
                        className="cursor-help"
                        tabIndex={0}
                        aria-label={trendTooltipText(point, legend)}
                        onFocus={() => setHoveredIndex(index)}
                        onBlur={() => setHoveredIndex(null)}
                        onPointerEnter={() => setHoveredIndex(index)}
                        onPointerLeave={() => setHoveredIndex(null)}
                    />
                ))}
                {labelIndexes.map((index) => (
                    <text
                        key={points[index].date}
                        x={coordinates[index].x}
                        y={height - 7}
                        textAnchor={index === 0 ? 'start' : index === points.length - 1 ? 'end' : 'middle'}
                        fill="currentColor"
                        opacity="0.62"
                        fontSize="11"
                    >
                        {formatChartDate(points[index].date)}
                    </text>
                ))}
                </svg>
                {hoveredIndex !== null && points[hoveredIndex] && (
                    <div
                        className="pointer-events-none absolute z-10 whitespace-nowrap border border-white/45 bg-[#11130f] px-2 py-1 text-[10px] font-bold text-white shadow-lg"
                        role="tooltip"
                        style={{
                            left: `${Math.min(Math.max(coordinates[hoveredIndex].x / width * 100, 24), 76)}%`,
                            top: `${Math.min(Math.max(coordinates[hoveredIndex].y / height * 100, 24), 78)}%`,
                            transform: 'translate(-50%, -115%)',
                        }}
                    >
                        {trendTooltipText(points[hoveredIndex], legend)}
                    </div>
                )}
            </div>
        </div>
    )
}

export function RankedBars({
    items,
    emptyMessage,
    valueFormatter = (value) => value.toLocaleString('ko-KR'),
    compact = false,
    emptyValue,
    slotCount,
    emptySlotLabel = '데이터 없음',
}: {
    items: Array<{ id: string | number; label: string; value: number; detail?: string; ranked?: boolean }>
    emptyMessage: string
    valueFormatter?: (value: number) => string
    compact?: boolean
    emptyValue?: string
    slotCount?: number
    emptySlotLabel?: string
}) {
    const max = Math.max(...items.map((item) => item.value), 1)
    if (items.length === 0 && !slotCount) {
        return (
            <DashboardEmpty>
                {emptyValue && <strong className="mb-1 block text-2xl text-ink dark:text-white">{emptyValue}</strong>}
                {emptyMessage}
            </DashboardEmpty>
        )
    }
    const rowCount = Math.max(slotCount ?? items.length, items.length)
    const rows = Array.from({ length: rowCount }, (_, index) => items[index] ?? null)

    return (
        <ol
            className="grid h-full"
            style={{ gridTemplateRows: `repeat(${rowCount}, minmax(0, 1fr))` }}
        >
            {rows.map((item, index) => (
                <li className="flex min-h-0 flex-col justify-center border-b border-line last:border-b-0" key={item?.id ?? `empty-${index}`}>
                    <div className={`${compact ? 'mb-px' : 'mb-0.5'} flex items-start justify-between gap-4 text-xs leading-tight`}>
                        <div className="min-w-0">
                            <span className="mr-2 text-[10px] font-black text-[#8ba127]">{item?.ranked === false ? '—' : String(index + 1).padStart(2, '0')}</span>
                            <strong className={item ? '' : 'text-muted'}>{item?.label ?? emptySlotLabel}</strong>
                            {item?.detail && compact && <span className="ml-2 text-[9px] font-normal text-muted">{item.detail}</span>}
                            {item?.detail && !compact && <p className="pl-7 text-[9px] leading-tight text-muted">{item.detail}</p>}
                        </div>
                        <b className={`shrink-0 tabular-nums ${item ? '' : 'text-muted'}`}>{valueFormatter(item?.value ?? 0)}</b>
                    </div>
                    <div className={`ml-7 ${compact ? 'h-0.5' : 'h-1'} bg-[#e9eadf] dark:bg-white/10`}>
                        <div className="h-full bg-[#8ba127]" style={{ width: `${(item?.value ?? 0) / max * 100}%` }} />
                    </div>
                </li>
            ))}
        </ol>
    )
}

export function DashboardEmpty({ children }: { children: ReactNode }) {
    return (
        <div className="grid min-h-36 place-content-center border border-dashed border-line px-5 text-center text-sm text-muted">
            {children}
        </div>
    )
}

function chartLabelIndexes(length: number) {
    if (length === 0) return []
    if (length <= 4) return Array.from({ length }, (_, index) => index)
    return [...new Set([0, Math.floor((length - 1) / 3), Math.floor((length - 1) * 2 / 3), length - 1])]
}

function formatChartDate(value: string) {
    const [, month, day] = value.split('-')
    return `${Number(month)}.${String(Number(day)).padStart(2, '0')}`
}

function trendTooltipText(point: DashboardTrendPoint, legend: string) {
    return `${formatChartDate(point.date)} · ${legend} ${formatPrice(point.netSalesAmount)} · 주문 ${point.orderCount.toLocaleString('ko-KR')}건 · 판매 ${point.salesQuantity.toLocaleString('ko-KR')}개`
}
