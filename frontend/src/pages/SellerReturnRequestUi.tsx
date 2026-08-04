import type { ReactNode } from 'react'
import { StatusBadge, type StatusBadgeTone } from '../components/ui/StatusBadge'
import type { ReturnRequestStatus } from '../types/order'
import { returnStatusLabel } from './sellerReturnStatus'

const returnStatusTones: Record<ReturnRequestStatus, StatusBadgeTone> = {
    REQUESTED: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
}

export function SellerReturnManagementPage({
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
            <p className="text-[10px] font-extrabold tracking-[.18em] text-accent">
                {eyebrow}
            </p>
            <div className="mb-8 mt-2">
                <h1 className="font-serif text-[clamp(34px,5vw,54px)] leading-none tracking-tighter">
                    {title}
                </h1>
                {description && <p className="mt-3 text-sm text-muted">{description}</p>}
            </div>
            {children}
        </section>
    )
}

export function ReturnStatusBadge({ status }: { status: ReturnRequestStatus }) {
    return (
        <StatusBadge tone={returnStatusTones[status]}>
            {returnStatusLabel[status]}
        </StatusBadge>
    )
}
