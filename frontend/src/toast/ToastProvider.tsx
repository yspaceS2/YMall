import { CircleAlert, CircleCheck, Info, X } from 'lucide-react'
import { useCallback, useMemo, useRef, useState, type ReactNode } from 'react'
import { ToastContext, type ToastTone } from './ToastContext'

interface ToastItem {
    id: number
    message: string
    tone: ToastTone
}

const toneStyles = {
    error: 'border-[#d9aaa4] bg-[#f9ecea] text-[#83281f] dark:border-[#7c4842] dark:bg-[#3a2725] dark:text-[#f0b7b1]',
    success: 'border-[#bdc998] bg-[#f2f5e7] text-[#4f5e18] dark:border-[#59643b] dark:bg-[#2c3226] dark:text-[#c8d799]',
    info: 'border-line bg-surface text-ink',
}

const toneIcons = {
    error: CircleAlert,
    success: CircleCheck,
    info: Info,
}

export function ToastProvider({ children }: { children: ReactNode }) {
    const [toasts, setToasts] = useState<ToastItem[]>([])
    const nextId = useRef(1)

    const dismissToast = useCallback((id: number) => {
        setToasts((current) => current.filter((toast) => toast.id !== id))
    }, [])

    const showToast = useCallback((message: string, tone: ToastTone = 'info') => {
        const normalizedMessage = message.trim()
        if (!normalizedMessage) return

        const id = nextId.current
        nextId.current += 1
        setToasts((current) => [
            ...current.slice(-2),
            { id, message: normalizedMessage, tone },
        ])
        window.setTimeout(() => dismissToast(id), 4_000)
    }, [dismissToast])

    const value = useMemo(() => ({ showToast }), [showToast])

    return (
        <ToastContext.Provider value={value}>
            {children}
            <div
                className="pointer-events-none fixed top-32 left-1/2 z-[100] flex w-[min(420px,calc(100vw-32px))] -translate-x-1/2 flex-col gap-2.5 min-[601px]:top-24"
                aria-label="알림 메시지"
            >
                {toasts.map((toast) => {
                    const Icon = toneIcons[toast.tone]
                    return (
                        <div
                            className={`pointer-events-auto flex items-start gap-3 border px-4 py-3.5 shadow-xl ${toneStyles[toast.tone]}`}
                            key={toast.id}
                            role={toast.tone === 'error' ? 'alert' : 'status'}
                        >
                            <Icon className="mt-0.5 size-4.5 shrink-0" aria-hidden="true" />
                            <p className="min-w-0 flex-1 text-sm leading-5.5">{toast.message}</p>
                            <button
                                className="grid size-6 shrink-0 place-items-center bg-transparent opacity-65 transition-opacity hover:opacity-100"
                                type="button"
                                aria-label="알림 닫기"
                                onClick={() => dismissToast(toast.id)}
                            >
                                <X className="size-4" aria-hidden="true" />
                            </button>
                        </div>
                    )
                })}
            </div>
        </ToastContext.Provider>
    )
}
