import { useEffect, useRef } from 'react'

interface ConfirmDialogProps {
    open: boolean
    title: string
    description: string
    confirmLabel?: string
    isPending?: boolean
    onCancel: () => void
    onConfirm: () => void
}

export function ConfirmDialog({
    open,
    title,
    description,
    confirmLabel = '확인',
    isPending = false,
    onCancel,
    onConfirm,
}: ConfirmDialogProps) {
    const cancelButtonRef = useRef<HTMLButtonElement>(null)

    useEffect(() => {
        if (!open) return
        cancelButtonRef.current?.focus()

        function handleKeyDown(event: KeyboardEvent) {
            if (event.key === 'Escape' && !isPending) onCancel()
        }

        document.addEventListener('keydown', handleKeyDown)
        return () => document.removeEventListener('keydown', handleKeyDown)
    }, [isPending, onCancel, open])

    if (!open) return null

    return (
        <div className="fixed inset-0 z-100 grid place-items-center bg-black/45 px-5" role="presentation" onMouseDown={(event) => {
            if (event.target === event.currentTarget && !isPending) onCancel()
        }}>
            <section className="w-full max-w-105 border border-ink bg-paper p-6 shadow-2xl min-[601px]:p-8" role="alertdialog" aria-modal="true" aria-labelledby="confirm-dialog-title" aria-describedby="confirm-dialog-description">
                <p className="text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">PLEASE CONFIRM</p>
                <h2 className="mt-3 font-serif text-3xl" id="confirm-dialog-title">{title}</h2>
                <p className="mt-4 text-sm leading-7 text-muted" id="confirm-dialog-description">{description}</p>
                <div className="mt-7 grid grid-cols-2 gap-3">
                    <button ref={cancelButtonRef} className="h-11 border border-ink bg-white text-xs font-bold disabled:opacity-50" type="button" disabled={isPending} onClick={onCancel}>취소</button>
                    <button className="h-11 border border-[#9d3026] bg-[#9d3026] text-xs font-bold text-white disabled:opacity-50" type="button" disabled={isPending} onClick={onConfirm}>{isPending ? '처리 중...' : confirmLabel}</button>
                </div>
            </section>
        </div>
    )
}
