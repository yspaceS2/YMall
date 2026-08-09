import { CircleAlert, CircleCheck, Info } from 'lucide-react'
import type { ReactNode } from 'react'

type FeedbackTone = 'error' | 'success' | 'info'

interface FeedbackMessageProps {
    tone: FeedbackTone
    children: ReactNode
    className?: string
}

const toneStyles = {
    error: 'border-danger/35 bg-danger-soft text-danger',
    success: 'border-success/35 bg-success-soft text-success',
    info: 'border-line bg-subtle text-muted',
}

const toneIcons = {
    error: CircleAlert,
    success: CircleCheck,
    info: Info,
}

export function FeedbackMessage({ tone, children, className = '' }: FeedbackMessageProps) {
    const Icon = toneIcons[tone]

    return (
        <p
            className={`flex items-start gap-2.5 border px-4 py-3 text-sm leading-6 ${toneStyles[tone]} ${className}`}
            role={tone === 'error' ? 'alert' : 'status'}
        >
            <Icon className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            <span>{children}</span>
        </p>
    )
}
