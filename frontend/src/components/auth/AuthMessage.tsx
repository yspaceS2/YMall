import type { ReactNode } from 'react'

interface AuthMessageProps {
    children: ReactNode
    tone?: 'error' | 'success' | 'info'
}

const toneClassNames = {
    error: 'border-[#d9aaa4] bg-[#f9ecea] text-[#9d3026]',
    success: 'border-[#a7b866] bg-[#eef3d8] text-[#55620f]',
    info: 'border-line bg-white text-muted',
}

export function AuthMessage({ children, tone = 'info' }: AuthMessageProps) {
    return (
        <p className={`border px-4 py-3 text-sm leading-6 ${toneClassNames[tone]}`} role={tone === 'error' ? 'alert' : 'status'}>
            {children}
        </p>
    )
}
