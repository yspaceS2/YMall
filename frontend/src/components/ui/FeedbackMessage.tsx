import { CircleAlert, CircleCheck, Info } from 'lucide-react'
import type { ReactNode } from 'react'

type FeedbackTone = 'error' | 'success' | 'info'

interface FeedbackMessageProps {
    tone: FeedbackTone
    children: ReactNode
    className?: string
}

const toneStyles = {
    error: 'border-[#d9aaa4] bg-[#f9ecea] text-[#9d3026]',
    success: 'border-[#bdc998] bg-[#f2f5e7] text-[#59691c]',
    info: 'border-[#c8c8bf] bg-[#f4f4ef] text-[#55554f]',
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
