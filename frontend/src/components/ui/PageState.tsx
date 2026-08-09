import { CircleAlert, Inbox, LoaderCircle } from 'lucide-react'
import type { ReactNode } from 'react'

type PageStateVariant = 'loading' | 'empty' | 'error'

interface PageStateProps {
    variant: PageStateVariant
    title: string
    description?: string
    action?: ReactNode
    compact?: boolean
}

const stateIcons = {
    loading: LoaderCircle,
    empty: Inbox,
    error: CircleAlert,
}

export function PageState({ variant, title, description, action, compact = false }: PageStateProps) {
    const Icon = stateIcons[variant]

    return (
        <div
            className={`grid place-content-center justify-items-center border-y border-line px-5 text-center ${compact ? 'min-h-40' : 'min-h-80'}`}
            role={variant === 'error' ? 'alert' : 'status'}
            aria-live={variant === 'loading' ? 'polite' : undefined}
        >
            <Icon
                className={`mb-4 size-8 ${variant === 'error' ? 'text-danger' : 'text-muted'} ${variant === 'loading' ? 'animate-spin' : ''}`}
                aria-hidden="true"
            />
            <strong className="text-ink">{title}</strong>
            {description && <p className="mt-2 max-w-120 text-sm leading-6 text-muted">{description}</p>}
            {action && <div className="mt-5">{action}</div>}
        </div>
    )
}
