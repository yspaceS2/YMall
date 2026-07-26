import { CheckCheck, LoaderCircle } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { PageState } from '../components/ui/PageState'
import {
    getNotifications,
    markAllNotificationsAsRead,
    markNotificationAsRead,
    notifyNotificationsChanged,
} from '../api/notifications'
import type { Notification } from '../types/notification'
import { formatKoreanDateTime } from '../utils/dateTime'

export function NotificationPage() {
    const [notifications, setNotifications] = useState<Notification[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [isLoadingMore, setIsLoadingMore] = useState(false)
    const [isUpdating, setIsUpdating] = useState(false)
    const [nextPage, setNextPage] = useState(2)
    const [hasNext, setHasNext] = useState(false)
    const [errorMessage, setErrorMessage] = useState('')
    const [successMessage, setSuccessMessage] = useState('')
    const [retryKey, setRetryKey] = useState(0)
    const loadMoreControllerRef = useRef<AbortController | null>(null)
    const navigate = useNavigate()
    const hasUnread = notifications.some((notification) => notification.readAt === null)

    useEffect(() => {
        const controller = new AbortController()
        getNotifications(1, 20, controller.signal)
            .then((response) => {
                setNotifications(response.content)
                setHasNext(response.hasNext)
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError ? error.message : '알림을 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => {
            controller.abort()
            loadMoreControllerRef.current?.abort()
        }
    }, [retryKey])

    async function loadMore() {
        if (!hasNext || isLoadingMore) return
        const controller = new AbortController()
        loadMoreControllerRef.current = controller
        setErrorMessage('')
        setSuccessMessage('')
        setIsLoadingMore(true)
        try {
            const response = await getNotifications(nextPage, 20, controller.signal)
            setNotifications((current) => [...current, ...response.content])
            setHasNext(response.hasNext)
            setNextPage((current) => current + 1)
        } catch (error) {
            if (error instanceof Error && error.name === 'AbortError') return
            setErrorMessage(error instanceof ApiError ? error.message : '알림을 불러오지 못했습니다.')
        } finally {
            if (!controller.signal.aborted) setIsLoadingMore(false)
        }
    }

    async function openNotification(notification: Notification) {
        setErrorMessage('')
        setSuccessMessage('')
        try {
            if (notification.readAt === null) {
                const updated = await markNotificationAsRead(notification.notificationId)
                setNotifications((current) => current.map((item) =>
                    item.notificationId === updated.notificationId ? updated : item
                ))
                notifyNotificationsChanged()
            }
            if (notification.targetUrl) navigate(notification.targetUrl)
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '알림을 처리하지 못했습니다.')
        }
    }

    async function readAll() {
        if (!hasUnread || isUpdating) return
        setErrorMessage('')
        setIsUpdating(true)
        try {
            await markAllNotificationsAsRead()
            const readAt = new Date().toISOString()
            setNotifications((current) => current.map((notification) => ({
                ...notification,
                readAt: notification.readAt ?? readAt,
            })))
            notifyNotificationsChanged()
            setSuccessMessage('모든 알림을 읽음 처리했습니다.')
        } catch (error) {
            setErrorMessage(error instanceof ApiError ? error.message : '알림을 처리하지 못했습니다.')
        } finally {
            setIsUpdating(false)
        }
    }

    if (isLoading) {
        return <PageState variant="loading" title="알림을 불러오는 중입니다" description="잠시만 기다려 주세요." />
    }

    if (errorMessage && notifications.length === 0) {
        return <PageState variant="error" title="알림을 불러오지 못했습니다" description={errorMessage} action={<button className="border border-ink bg-white px-5 py-2.5 text-xs font-bold" type="button" onClick={() => { setErrorMessage(''); setIsLoading(true); setRetryKey((value) => value + 1) }}>다시 시도</button>} />
    }

    return (
        <section className="mx-auto max-w-220 px-4 py-14 min-[601px]:px-8 min-[601px]:py-20">
            <div className="mb-10 flex flex-wrap items-end justify-between gap-5">
                <div>
                    <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">NOTIFICATIONS</p>
                    <h1 className="font-serif text-[clamp(42px,6vw,68px)] leading-none tracking-tighter">알림</h1>
                </div>
                {notifications.length > 0 && (
                    <button
                        className="inline-flex items-center gap-2 border border-ink bg-white px-4 py-2.5 text-xs font-bold disabled:opacity-40"
                        type="button"
                        disabled={!hasUnread || isUpdating}
                        onClick={readAll}
                    >
                        <CheckCheck className="size-4" />
                        모두 읽음
                    </button>
                )}
            </div>

            {errorMessage && <FeedbackMessage className="mb-5" tone="error">{errorMessage}</FeedbackMessage>}
            {successMessage && <FeedbackMessage className="mb-5" tone="success">{successMessage}</FeedbackMessage>}

            {notifications.length === 0 ? (
                <PageState variant="empty" title="새로운 알림이 없습니다" description="주문과 배송 상태가 바뀌면 이곳에서 알려드립니다." />
            ) : (
                <div className="border-t border-ink">
                    {notifications.map((notification) => (
                        <button
                            className={`grid w-full grid-cols-[8px_1fr] gap-4 border-b border-line px-1 py-6 text-left transition-colors hover:bg-[#f7f7f1] ${notification.readAt === null ? 'bg-[#f3f5e8]' : 'bg-white'}`}
                            type="button"
                            key={notification.notificationId}
                            onClick={() => openNotification(notification)}
                        >
                            <span className={`mt-1.5 size-2 rounded-full ${notification.readAt === null ? 'bg-[#849b21]' : 'bg-transparent'}`} />
                            <span>
                                <span className="flex flex-wrap items-center justify-between gap-3">
                                    <strong className="text-sm">{notification.title}</strong>
                                    <time className="text-[11px] text-muted">{formatNotificationDate(notification.createdAt)}</time>
                                </span>
                                <span className="mt-2 block text-sm leading-6 text-[#66665f]">{notification.message}</span>
                            </span>
                        </button>
                    ))}
                    {hasNext && (
                        <button
                            className="mx-auto mt-8 grid h-11 min-w-36 place-items-center border border-ink bg-white px-6 text-xs font-bold disabled:opacity-50"
                            type="button"
                            disabled={isLoadingMore}
                            onClick={loadMore}
                        >
                            {isLoadingMore ? <LoaderCircle className="size-4 animate-spin" /> : '알림 더 보기'}
                        </button>
                    )}
                </div>
            )}
        </section>
    )
}

function formatNotificationDate(value: string) {
    return formatKoreanDateTime(value)
}
