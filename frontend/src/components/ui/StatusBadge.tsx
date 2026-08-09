import type { ReactNode } from 'react'

export type StatusBadgeTone = 'neutral' | 'info' | 'warning' | 'success' | 'danger'

const toneClassNames: Record<StatusBadgeTone, string> = {
    neutral: 'border-line bg-paper text-muted',
    info: 'border-info/30 bg-info-soft text-info',
    warning: 'border-warning/30 bg-warning-soft text-warning',
    success: 'border-success/30 bg-success-soft text-success',
    danger: 'border-danger/30 bg-danger-soft text-danger',
}

export function StatusBadge({
    children,
    tone = 'neutral',
    className = '',
}: {
    children: ReactNode
    tone?: StatusBadgeTone
    className?: string
}) {
    return (
        <span className={`inline-flex w-fit items-center rounded-full border px-2.5 py-1 text-[10px] font-extrabold leading-none ${toneClassNames[tone]} ${className}`}>
            {children}
        </span>
    )
}
