import { CheckCheck, LoaderCircle, Trash2 } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { PageState } from '../components/ui/PageState'
import {
    deleteAllNotifications,
    deleteNotification,
    getNotifications,
    markAllNotificationsAsRead,
    markNotificationAsRead,
    notifyNotificationsChanged,
} from '../api/notifications'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
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
    const [notificationToDelete, setNotificationToDelete] = useState<Notification | null>(null)
    const [isDeleteAllDialogOpen, setIsDeleteAllDialogOpen] = useState(false)
    const [isDeleting, setIsDeleting] = useState(false)
    const loadMoreControllerRef = useRef<AbortController | null>(null)
    const navigate = useNavigate()
    const location = useLocation()
    const isMemberNotificationPage = location.pathname === '/mypage/notifications'
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
            setErrorMessage(
                error instanceof ApiError ? error.message : '알림을 불러오지 못했습니다.',
            )
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
            setErrorMessage(
                error instanceof ApiError ? error.message : '알림을 처리하지 못했습니다.',
            )
        }
    }

    async function readAll() {
        if (!hasUnread || isUpdating) return
        setErrorMessage('')
        setSuccessMessage('')
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
            setErrorMessage(
                error instanceof ApiError ? error.message : '알림을 처리하지 못했습니다.',
            )
        } finally {
            setIsUpdating(false)
        }
    }

    async function deleteSelectedNotification() {
        if (notificationToDelete === null || isDeleting) return
        setIsDeleting(true)
        setErrorMessage('')
        setSuccessMessage('')
        try {
            await deleteNotification(notificationToDelete.notificationId)
            setNotifications((current) => current.filter(
                (notification) =>
                    notification.notificationId !== notificationToDelete.notificationId,
            ))
            setNotificationToDelete(null)
            notifyNotificationsChanged()
            setSuccessMessage('알림을 삭제했습니다.')
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError ? error.message : '알림을 삭제하지 못했습니다.',
            )
        } finally {
            setIsDeleting(false)
        }
    }

    async function deleteEveryNotification() {
        if (isDeleting) return
        setIsDeleting(true)
        setErrorMessage('')
        setSuccessMessage('')
        try {
            await deleteAllNotifications()
            setNotifications([])
            setHasNext(false)
            setIsDeleteAllDialogOpen(false)
            notifyNotificationsChanged()
            setSuccessMessage('모든 알림을 삭제했습니다.')
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError ? error.message : '알림을 모두 삭제하지 못했습니다.',
            )
        } finally {
            setIsDeleting(false)
        }
    }

    if (isLoading) {
        return <PageState variant="loading" title="알림을 불러오는 중입니다" description="잠시만 기다려 주세요." />
    }

    if (errorMessage && notifications.length === 0) {
        return <PageState variant="error" title="알림을 불러오지 못했습니다" description={errorMessage} action={<button className="border border-ink bg-surface px-5 py-2.5 text-xs font-bold" type="button" onClick={() => { setErrorMessage(''); setIsLoading(true); setRetryKey((value) => value + 1) }}>다시 시도</button>} />
    }

    return (
        <section className="mx-auto max-w-220 px-4 py-14 min-[601px]:px-8 min-[601px]:py-20">
            <div className="mb-10 flex flex-wrap items-end justify-between gap-5">
                <div>
                    <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-[#71801e] dark:text-[#c9db72]">NOTIFICATIONS</p>
                    <h1 className="font-serif text-[clamp(42px,6vw,68px)] leading-none tracking-tighter">알림</h1>
                </div>
                {notifications.length > 0 && (
                    <div className="flex flex-wrap gap-2">
                        <button
                            className="inline-flex items-center gap-2 border border-ink bg-surface px-4 py-2.5 text-xs font-bold transition-colors hover:bg-paper disabled:opacity-40"
                            type="button"
                            disabled={!hasUnread || isUpdating}
                            onClick={readAll}
                        >
                            <CheckCheck className="size-4" />
                            모두 읽음
                        </button>
                        {isMemberNotificationPage && (
                            <button
                                className="inline-flex items-center gap-2 border border-[#9d3026] bg-surface px-4 py-2.5 text-xs font-bold text-[#9d3026] transition-colors hover:bg-[#9d3026]/10"
                                type="button"
                                onClick={() => setIsDeleteAllDialogOpen(true)}
                            >
                                <Trash2 className="size-4" />
                                모두 삭제
                            </button>
                        )}
                    </div>
                )}
            </div>

            {errorMessage && notifications.length > 0 && (
                <FeedbackMessage className="mb-5" tone="error">
                    {errorMessage}
                </FeedbackMessage>
            )}
            {successMessage && (
                <FeedbackMessage className="mb-5" tone="success">
                    {successMessage}
                </FeedbackMessage>
            )}

            {notifications.length === 0 ? (
                <PageState variant="empty" title="새로운 알림이 없습니다" description="주문과 배송 상태가 바뀌면 이곳에서 알려드립니다." />
            ) : (
                <div className="border-t border-ink">
                    {notifications.map((notification) => (
                        <article
                            className={`grid grid-cols-[8px_minmax(0,1fr)_auto] gap-4 border-b border-line px-1 py-6 transition-colors hover:bg-paper ${notification.readAt === null ? 'bg-[#f3f5e8] dark:bg-[#303427]' : 'bg-surface'}`}
                            key={notification.notificationId}
                        >
                            <span className={`mt-1.5 size-2 rounded-full ${notification.readAt === null ? 'bg-[#849b21] dark:bg-lime' : 'bg-transparent'}`} />
                            <button
                                className="min-w-0 text-left"
                                type="button"
                                onClick={() => openNotification(notification)}
                            >
                                <span className="flex flex-wrap items-center justify-between gap-3">
                                    <strong className="text-sm">{notification.title}</strong>
                                    <time className="text-[11px] text-muted">{formatNotificationDate(notification.createdAt)}</time>
                                </span>
                                <span className="mt-2 block text-sm leading-6 text-muted">{notification.message}</span>
                            </button>
                            {isMemberNotificationPage && (
                                <button
                                    className="grid size-9 place-items-center rounded-full text-muted transition-colors hover:bg-[#9d3026]/10 hover:text-[#9d3026]"
                                    type="button"
                                    aria-label={`${notification.title} 알림 삭제`}
                                    onClick={() => setNotificationToDelete(notification)}
                                >
                                    <Trash2 className="size-4" aria-hidden="true" />
                                </button>
                            )}
                        </article>
                    ))}
                    {hasNext && (
                        <button
                            className="mx-auto mt-8 grid h-11 min-w-36 place-items-center border border-ink bg-surface px-6 text-xs font-bold transition-colors hover:bg-paper disabled:opacity-50"
                            type="button"
                            disabled={isLoadingMore}
                            onClick={loadMore}
                        >
                            {isLoadingMore ? <LoaderCircle className="size-4 animate-spin" /> : '알림 더 보기'}
                        </button>
                    )}
                </div>
            )}
            <ConfirmDialog
                open={notificationToDelete !== null}
                title="알림을 삭제할까요?"
                description="삭제한 알림은 다시 복구할 수 없습니다."
                confirmLabel="삭제"
                isPending={isDeleting}
                onCancel={() => setNotificationToDelete(null)}
                onConfirm={deleteSelectedNotification}
            />
            <ConfirmDialog
                open={isDeleteAllDialogOpen}
                title="모든 알림을 삭제할까요?"
                description="현재 계정의 모든 알림이 삭제되며 다시 복구할 수 없습니다."
                confirmLabel="모두 삭제"
                isPending={isDeleting}
                onCancel={() => setIsDeleteAllDialogOpen(false)}
                onConfirm={deleteEveryNotification}
            />
        </section>
    )
}

function formatNotificationDate(value: string) {
    return formatKoreanDateTime(value)
}
