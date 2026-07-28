import type { ReactNode } from 'react'
import { useEffect, useState } from 'react'
import { getUnreadNotificationCount, NOTIFICATIONS_CHANGED_EVENT } from '../api/notifications'
import ymallSymbolLight from '../assets/brand/ymall-symbol-light.svg'
import { useAuth } from '../auth/useAuth'
import { StoreHeader } from './StoreHeader'

export function Layout({ children }: { children: ReactNode }) {
    const { isAuthenticated, role, logout } = useAuth()
    const [unreadCount, setUnreadCount] = useState(0)

    useEffect(() => {
        if (!isAuthenticated) {
            return
        }
        let controller: AbortController | null = null
        const loadUnreadCount = () => {
            controller?.abort()
            controller = new AbortController()
            getUnreadNotificationCount(controller.signal)
                .then((response) => setUnreadCount(response.unreadCount))
                .catch((error: unknown) => {
                    if (error instanceof Error && error.name === 'AbortError') return
                    setUnreadCount(0)
                })
        }
        loadUnreadCount()
        const intervalId = window.setInterval(loadUnreadCount, 30_000)
        window.addEventListener(NOTIFICATIONS_CHANGED_EVENT, loadUnreadCount)
        return () => {
            controller?.abort()
            window.clearInterval(intervalId)
            window.removeEventListener(NOTIFICATIONS_CHANGED_EVENT, loadUnreadCount)
        }
    }, [isAuthenticated])

    const handleLogout = async () => {
        setUnreadCount(0)
        await logout()
    }

    return (
        <div className="flex min-h-screen flex-col bg-paper text-ink">
            <StoreHeader
                isAuthenticated={isAuthenticated}
                role={role}
                unreadCount={unreadCount}
                onLogout={handleLogout}
            />
            <main className="flex-1">{children}</main>
            <footer className="flex flex-col items-start gap-6 bg-[#1d1d1b] px-5 py-10 text-xs text-[#bcbcb5] min-[601px]:px-[clamp(24px,5vw,80px)] min-[601px]:py-12 min-[901px]:flex-row min-[901px]:items-end min-[901px]:justify-between">
                <div>
                    <div className="flex items-center gap-2">
                        <img className="size-10" src={ymallSymbolLight} alt="" aria-hidden="true" />
                        <strong className="font-serif text-2xl tracking-[.08em] text-white">YMALL</strong>
                    </div>
                    <p className="mt-2.5">일상에 취향을 더하는 셀렉트 스토어</p>
                </div>
                <p>© 2026 YMall. Portfolio project.</p>
            </footer>
        </div>
    )
}
