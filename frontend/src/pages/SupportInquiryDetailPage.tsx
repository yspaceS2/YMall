import { ArrowLeft, LoaderCircle } from 'lucide-react'
import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
    acceptLiveSupport,
    addSupportMessage,
    cancelLiveSupport,
    closeSupportInquiry,
    endLiveSupport,
    getSupportInquiry,
    offerLiveSupport,
    rejectLiveSupport,
    requestLiveSupport,
} from '../api/support'
import { useAuth } from '../auth/useAuth'
import { getAccessToken, getTokenSubject } from '../auth/tokenStorage'
import { PageState } from '../components/ui/PageState'
import { REALTIME_EVENT } from '../realtime/RealtimeProvider'
import { useRealtime } from '../realtime/useRealtime'
import { useToast } from '../toast/useToast'
import type {
    RealtimeEvent,
    SupportInquiryDetail,
    SupportMessage,
} from '../types/support'
import { InquiryDetail } from './SupportCenterPage'
import { supportBasePath } from './supportInquiryRouting'

export function SupportInquiryDetailPage({ admin = false }: { admin?: boolean }) {
    const { role } = useAuth()
    const { showToast } = useToast()
    const navigate = useNavigate()
    const { connected, connectionVersion, publish, subscribe } = useRealtime()
    const { inquiryId: inquiryIdParam } = useParams()
    const inquiryId = Number(inquiryIdParam)
    const invalidId = !Number.isInteger(inquiryId) || inquiryId <= 0
    const [detail, setDetail] = useState<SupportInquiryDetail | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [message, setMessage] = useState('')
    const [attachments, setAttachments] = useState<File[]>([])
    const [resolution, setResolution] = useState('')
    const basePath = supportBasePath(admin, role)
    const currentMemberId = Number(getTokenSubject(getAccessToken())) || null

    const loadDetail = useCallback(async (signal?: AbortSignal, showLoading = true) => {
        if (invalidId) {
            setLoading(false)
            return
        }
        if (showLoading) setLoading(true)
        try {
            setDetail(await getSupportInquiry(admin, inquiryId, signal))
            setError('')
        } catch (loadError) {
            if (loadError instanceof Error && loadError.name === 'AbortError') return
            setError(loadError instanceof Error ? loadError.message : '문의 내용을 불러오지 못했습니다.')
        } finally {
            if (showLoading && !signal?.aborted) setLoading(false)
        }
    }, [admin, inquiryId, invalidId])

    const mergeMessage = useCallback((incoming: SupportMessage) => {
        setDetail((current) => {
            if (!current || current.messages.some((item) => item.messageId === incoming.messageId)) return current
            return { ...current, messages: [...current.messages, incoming] }
        })
    }, [])

    useEffect(() => {
        const controller = new AbortController()
        const timerId = window.setTimeout(() => void loadDetail(controller.signal), 0)
        return () => {
            window.clearTimeout(timerId)
            controller.abort()
        }
    }, [loadDetail])

    useEffect(() => {
        if (invalidId || !connected) return
        return subscribe(`/topic/support/inquiries/${inquiryId}`, (body) => {
            const event = JSON.parse(body) as Partial<SupportMessage>
            if (typeof event.messageId === 'number') mergeMessage(event as SupportMessage)
            else void loadDetail(undefined, false)
        })
    }, [connected, connectionVersion, inquiryId, invalidId, loadDetail, mergeMessage, subscribe])

    useEffect(() => {
        const refresh = (event?: Event) => {
            const realtime = (event as CustomEvent<RealtimeEvent> | undefined)?.detail
            if (!realtime || realtime.resourceId === inquiryId) void loadDetail(undefined, false)
        }
        const intervalId = window.setInterval(refresh, 30_000)
        window.addEventListener('focus', refresh)
        window.addEventListener(REALTIME_EVENT, refresh)
        return () => {
            window.clearInterval(intervalId)
            window.removeEventListener('focus', refresh)
            window.removeEventListener(REALTIME_EVENT, refresh)
        }
    }, [inquiryId, loadDetail])

    const runAction = async (action: () => Promise<SupportInquiryDetail>, successMessage: string) => {
        setSubmitting(true)
        try {
            setDetail(await action())
            showToast(successMessage, 'success')
        } catch (actionError) {
            showToast(actionError instanceof Error ? actionError.message : '요청을 처리하지 못했습니다.', 'error')
        } finally {
            setSubmitting(false)
        }
    }

    const submitMessage = async (event: FormEvent) => {
        event.preventDefault()
        if (!detail || (!message.trim() && attachments.length === 0)) return
        const content = message.trim()
        const files = attachments
        setSubmitting(true)
        try {
            if (detail.inquiry.status === 'LIVE_ACTIVE' && files.length === 0) {
                const sent = publish(`/app/support/inquiries/${inquiryId}/messages`, {
                    clientMessageId: crypto.randomUUID(),
                    content,
                })
                if (!sent) throw new Error('실시간 연결을 복구한 뒤 다시 시도해 주세요.')
            } else {
                mergeMessage(await addSupportMessage(admin, inquiryId, content, files))
            }
            setMessage('')
            setAttachments([])
        } catch (submitError) {
            showToast(submitError instanceof Error ? submitError.message : '메시지를 전송하지 못했습니다.', 'error')
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <section className="mx-auto flex h-[calc(100dvh-5.5rem)] max-w-350 flex-col overflow-hidden px-4 py-4 min-[601px]:px-8 min-[601px]:py-6">
            <Link className="mb-4 inline-flex w-fit shrink-0 items-center gap-2 text-xs font-bold underline underline-offset-4" to={basePath}>
                <ArrowLeft className="size-4" /> 문의 목록
            </Link>
            {loading ? (
                <div className="grid min-h-72 place-items-center"><LoaderCircle className="size-6 animate-spin" aria-label="불러오는 중" /></div>
            ) : invalidId || error || !detail ? (
                <PageState variant="error" title={error || '문의 정보를 확인할 수 없습니다.'} />
            ) : (
                <div className="min-h-0 flex-1 overflow-hidden border border-line bg-surface">
                    <InquiryDetail
                        admin={admin}
                        connected={connected}
                        currentMemberId={currentMemberId}
                        detail={detail}
                        message={message}
                        attachments={attachments}
                        resolution={resolution}
                        submitting={submitting}
                        onMessageChange={setMessage}
                        onAttachmentsChange={setAttachments}
                        onResolutionChange={setResolution}
                        onSubmitMessage={submitMessage}
                        onRequestLive={() => void runAction(() => requestLiveSupport(inquiryId), '실시간 상담을 요청했습니다.')}
                        onOfferLive={() => void runAction(() => offerLiveSupport(inquiryId), '실시간 상담을 제안했습니다.')}
                        onAcceptLive={() => void runAction(() => acceptLiveSupport(admin, inquiryId), '실시간 상담을 시작했습니다.')}
                        onRejectLive={() => void runAction(() => rejectLiveSupport(admin, inquiryId), '실시간 상담 요청을 거절했습니다.')}
                        onCancelLive={() => void runAction(() => cancelLiveSupport(admin, inquiryId), '실시간 상담 요청을 취소했습니다.')}
                        onEndLive={() => void runAction(() => endLiveSupport(admin, inquiryId), '실시간 상담을 종료했습니다.')}
                        onClose={() => {
                            if (!resolution.trim()) return
                            void runAction(() => closeSupportInquiry(inquiryId, resolution.trim()), '문의 처리를 완료했습니다.')
                                .then(() => setResolution(''))
                        }}
                        onCreateNew={!admin ? () => navigate(`${basePath}?create=true`) : undefined}
                    />
                </div>
            )}
        </section>
    )
}
