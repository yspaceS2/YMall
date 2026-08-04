import { PackageCheck, RotateCcw } from 'lucide-react'
import type { ReactNode } from 'react'
import type { FulfillmentStatus, SellerOrderItem } from '../types/seller'
import { StatusBadge } from '../components/ui/StatusBadge'
import { statusLabels } from './sellerOrderStatus'

export function ManagementPage({
    eyebrow,
    title,
    description,
    children,
}: {
    eyebrow: string
    title: string
    description?: string
    children: ReactNode
}) {
    return (
        <section className="mx-auto max-w-350 px-4 py-10 min-[601px]:px-8 min-[601px]:py-14">
            <p className="text-[10px] font-extrabold tracking-[.18em] text-accent">{eyebrow}</p>
            <div className="mb-8 mt-2">
                <h1 className="font-serif text-[clamp(34px,5vw,54px)] leading-none tracking-tighter">{title}</h1>
                {description && <p className="mt-3 text-sm text-muted">{description}</p>}
            </div>
            {children}
        </section>
    )
}

export function ProductThumbnail({ item, large = false }: { item?: SellerOrderItem; large?: boolean }) {
    const size = large ? 'size-22' : 'size-14'
    return item?.thumbnailUrl ? (
        <img
            className={`${size} shrink-0 object-cover`}
            src={item.thumbnailUrl}
            alt=""
        />
    ) : (
        <span className={`${size} grid shrink-0 place-items-center bg-paper text-muted`}>
            <PackageCheck className="size-5" />
        </span>
    )
}

export function FulfillmentStatusBadge({ status }: { status: FulfillmentStatus }) {
    const tone = status === 'DELIVERED'
        ? 'success'
        : status === 'SHIPPED'
            ? 'info'
            : status === 'PREPARING'
                ? 'warning'
                : 'neutral'
    return (
        <StatusBadge tone={tone}>
            {statusLabels[status]}
        </StatusBadge>
    )
}

export function InfoTerm({ label, value }: { label: string; value: string }) {
    return (
        <div className="grid gap-1 min-[501px]:grid-cols-[80px_1fr]">
            <dt className="text-xs font-bold text-muted">{label}</dt>
            <dd>{value}</dd>
        </div>
    )
}

export function ActionButton({
    label,
    danger = false,
    disabled,
    onClick,
}: {
    label: string
    danger?: boolean
    disabled: boolean
    onClick: () => void
}) {
    return (
        <button
            className={[
                'h-10 border px-4 text-xs font-bold disabled:opacity-40',
                danger
                    ? 'border-danger text-danger'
                    : 'border-ink text-ink',
            ].join(' ')}
            type="button"
            disabled={disabled}
            onClick={onClick}
        >
            {danger ? <RotateCcw className="mr-1.5 inline size-3.5" /> : null}
            {label}
        </button>
    )
}
