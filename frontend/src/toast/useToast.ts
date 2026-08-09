import { useContext } from 'react'
import { ToastContext } from './ToastContext'

export function useToast() {
    const context = useContext(ToastContext)
    if (!context) {
        throw new Error('useToast는 ToastProvider 안에서 사용해야 합니다.')
    }
    return context
}
