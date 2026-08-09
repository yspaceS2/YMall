import type { InputHTMLAttributes, ReactNode } from 'react'

interface AuthFieldProps extends InputHTMLAttributes<HTMLInputElement> {
    id: string
    label: string
    message?: ReactNode
    messageId?: string
    action?: ReactNode
}

export const authInputClassName = 'w-full min-w-0 border-0 border-b border-line bg-transparent px-0.5 py-3.5 text-sm text-ink outline-0 transition-colors placeholder:text-muted focus:border-ink focus-visible:ring-2 focus-visible:ring-accent/35 disabled:text-muted aria-[invalid=true]:border-danger'

export function AuthField({ id, label, message, messageId, action, ...inputProps }: AuthFieldProps) {
    return (
        <div className="grid gap-2 text-xs font-bold text-muted">
            <label htmlFor={id}>{label}</label>
            <div className={action ? 'grid gap-2 min-[481px]:grid-cols-[minmax(0,1fr)_auto] min-[481px]:items-end' : undefined}>
                <input
                    {...inputProps}
                    id={id}
                    className={`${authInputClassName} ${inputProps.className ?? ''}`}
                    aria-describedby={message ? messageId : inputProps['aria-describedby']}
                />
                {action}
            </div>
            {message && <div id={messageId}>{message}</div>}
        </div>
    )
}
