import { useEffect, useState } from 'react'
import {
    getUnreadNotificationCount,
    NOTIFICATIONS_CHANGED_EVENT,
} from '../../api/notifications'
import {
    getSellerPendingQuestionCount,
    SELLER_QUESTION_COUNT_CHANGED_EVENT,
} from '../../api/productQuestions'
import {
    getSellerPendingOrderCount,
    SELLER_PENDING_ORDER_COUNT_CHANGED_EVENT,
} from '../../api/seller'
import { getPendingSupportCount } from '../../api/support'
import { REALTIME_EVENT } from '../../realtime/RealtimeProvider'
import type { ManagementRole } from './managementNavigation'

const REFRESH_INTERVAL_MS = 30_000

interface BadgeCountOptions {
    enabled: boolean
    load: (signal: AbortSignal) => Promise<number>
    changedEvent: string
    refreshOnFocus?: boolean
}

function useBadgeCount({
    enabled,
    load,
    changedEvent,
    refreshOnFocus = false,
}: BadgeCountOptions) {
    const [count, setCount] = useState(0)

    useEffect(() => {
        if (!enabled) return

        let active = true
        let controller: AbortController | null = null
        const refresh = () => {
            controller?.abort()
            controller = new AbortController()
            load(controller.signal)
                .then((nextCount) => {
                    if (active) setCount(nextCount)
                })
                .catch((error: unknown) => {
                    if (error instanceof Error && error.name === 'AbortError') return
                    if (active) setCount(0)
                })
        }

        refresh()
        const intervalId = window.setInterval(refresh, REFRESH_INTERVAL_MS)
        window.addEventListener(changedEvent, refresh)
        if (refreshOnFocus) window.addEventListener('focus', refresh)
        return () => {
            active = false
            controller?.abort()
            window.clearInterval(intervalId)
            window.removeEventListener(changedEvent, refresh)
            if (refreshOnFocus) window.removeEventListener('focus', refresh)
        }
    }, [changedEvent, enabled, load, refreshOnFocus])

    return enabled ? count : 0
}

const loadUnreadNotificationCount = (signal: AbortSignal) =>
    getUnreadNotificationCount(signal).then((response) => response.unreadCount)

const loadPendingQuestionCount = (signal: AbortSignal) =>
    getSellerPendingQuestionCount(signal).then((response) => response.count)

const loadPendingOrderCount = (signal: AbortSignal) =>
    getSellerPendingOrderCount(signal).then((response) => response.count)

const loadPendingSupportCount = (signal: AbortSignal) =>
    getPendingSupportCount(signal).then((response) => response.count)

export function useManagementBadgeCounts(
    role: ManagementRole,
    canReadAdminSupport: boolean,
) {
    const unreadNotificationCount = useBadgeCount({
        enabled: true,
        load: loadUnreadNotificationCount,
        changedEvent: NOTIFICATIONS_CHANGED_EVENT,
    })
    const pendingQuestionCount = useBadgeCount({
        enabled: role === 'seller',
        load: loadPendingQuestionCount,
        changedEvent: SELLER_QUESTION_COUNT_CHANGED_EVENT,
    })
    const pendingOrderCount = useBadgeCount({
        enabled: role === 'seller',
        load: loadPendingOrderCount,
        changedEvent: SELLER_PENDING_ORDER_COUNT_CHANGED_EVENT,
    })
    const pendingSupportCount = useBadgeCount({
        enabled: role === 'admin' && canReadAdminSupport,
        load: loadPendingSupportCount,
        changedEvent: REALTIME_EVENT,
        refreshOnFocus: true,
    })

    return {
        pendingOrderCount,
        pendingQuestionCount,
        pendingSupportCount,
        unreadNotificationCount,
    }
}
