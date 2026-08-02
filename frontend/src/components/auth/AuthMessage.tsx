import type { ReactNode } from 'react'

interface AuthMessageProps {
    children: ReactNode
    tone?: 'error' | 'success' | 'info'
}

const toneClassNames = {
    error: 'border-danger/35 bg-danger-soft text-danger',
    success: 'border-success/35 bg-success-soft text-success',
    info: 'border-line bg-subtle text-muted',
}

export function AuthMessage({ children, tone = 'info' }: AuthMessageProps) {
    return (
        <p className={`border px-4 py-3 text-sm leading-6 ${toneClassNames[tone]}`} role={tone === 'error' ? 'alert' : 'status'}>
            {children}
        </p>
    )
}
