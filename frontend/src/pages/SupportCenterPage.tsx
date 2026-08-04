import {
    CheckCircle2,
    CircleDot,
    FileText,
    Headphones,
    Image as ImageIcon,
    MessageCircleMore,
    Paperclip,
    Plus,
    Radio,
    Send,
    UserRoundCheck,
    X,
} from 'lucide-react'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { getAccessToken } from '../auth/tokenStorage'
import type {
    SupportAttachment,
    SupportInquiryCategory,
    SupportInquiryCreateRequest,
    SupportInquiryDetail,
    SupportInquiryStatus,
} from '../types/support'
import { formatKoreanDateTime } from '../utils/dateTime'
import { CATEGORY_LABELS, STATUS_LABELS } from './supportPresentation'
import { StatusBadge as CommonStatusBadge, type StatusBadgeTone } from '../components/ui/StatusBadge'

const CUSTOMER_CATEGORIES: SupportInquiryCategory[] = [
    'ORDER', 'PAYMENT', 'CANCEL_REFUND', 'DELIVERY', 'ACCOUNT', 'SERVICE',
]
const SELLER_CATEGORIES: SupportInquiryCategory[] = [
    'PRODUCT_APPROVAL', 'SETTLEMENT', 'SELLER_PERMISSION', 'POLICY', 'ACCOUNT', 'SERVICE',
]

const SUPPORT_ATTACHMENT_TYPES = new Set([
    'image/jpeg', 'image/png', 'image/webp', 'application/pdf',
])
const MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024
const MAX_ATTACHMENTS = 5

export function InquiryCreateForm({
    seller,
    submitting,
    onCancel,
    onSubmit,
}: {
    seller: boolean
    submitting: boolean
    onCancel: () => void
    onSubmit: (request: SupportInquiryCreateRequest) => Promise<void>
}) {
    const categories = seller ? SELLER_CATEGORIES : CUSTOMER_CATEGORIES
    const [category, setCategory] = useState<SupportInquiryCategory>(categories[0])
    const [title, setTitle] = useState('')
    const [content, setContent] = useState('')

    return (
        <form
            className="mx-auto grid max-w-180 gap-5 p-6 min-[601px]:p-10"
            onSubmit={(event) => {
                event.preventDefault()
                void onSubmit({ category, title: title.trim(), content: content.trim() })
            }}
        >
            <div className="flex items-center justify-between gap-3">
                <div>
                    <p className="text-xs font-extrabold tracking-[.14em] text-accent">NEW TICKET</p>
                    <h2 className="mt-2 text-2xl font-extrabold">새 문의</h2>
                </div>
                <button className="grid size-10 place-items-center border border-line" type="button" aria-label="문의 작성 취소" onClick={onCancel}>
                    <X className="size-4" />
                </button>
            </div>
            <label className="grid gap-2 text-sm font-bold">
                문의 유형
                <select className="h-12 border border-line bg-paper px-4 font-normal outline-none focus:border-ink" value={category} onChange={(event) => setCategory(event.target.value as SupportInquiryCategory)}>
                    {categories.map((value) => <option key={value} value={value}>{CATEGORY_LABELS[value]}</option>)}
                </select>
            </label>
            <label className="grid gap-2 text-sm font-bold">
                제목
                <input className="h-12 border border-line bg-paper px-4 font-normal outline-none focus:border-ink" required maxLength={120} value={title} onChange={(event) => setTitle(event.target.value)} placeholder="문의 제목을 입력해 주세요" />
            </label>
            <label className="grid gap-2 text-sm font-bold">
                문의 내용
                <textarea className="min-h-60 resize-none border border-line bg-paper p-4 font-normal leading-7 outline-none focus:border-ink" required maxLength={2000} value={content} onChange={(event) => setContent(event.target.value)} placeholder="문의 내용을 자세히 적어 주세요" />
                <span className="justify-self-end text-xs font-normal text-muted">{content.length} / 2,000</span>
            </label>
            <div className="flex justify-end gap-2">
                <button className="border border-line px-5 py-3 text-sm font-bold" type="button" onClick={onCancel}>취소</button>
                <button className="bg-ink px-6 py-3 text-sm font-bold text-paper disabled:opacity-50" type="submit" disabled={submitting || !title.trim() || !content.trim()}>
                    문의 등록
                </button>
            </div>
        </form>
    )
}

interface InquiryDetailProps {
    admin: boolean
    connected: boolean
    currentMemberId: number | null
    detail: SupportInquiryDetail
    message: string
    attachments: File[]
    resolution: string
    submitting: boolean
    onMessageChange: (value: string) => void
    onAttachmentsChange: (files: File[]) => void
    onResolutionChange: (value: string) => void
    onSubmitMessage: (event: FormEvent) => void
    onRequestLive: () => void
    onOfferLive: () => void
    onAcceptLive: () => void
    onRejectLive: () => void
    onCancelLive: () => void
    onEndLive: () => void
    onClose: () => void
    onCreateNew?: () => void
}

export function InquiryDetail(props: InquiryDetailProps) {
    const { detail, admin } = props
    const inquiry = detail.inquiry
    const session = detail.chatSession
    const liveActive = inquiry.status === 'LIVE_ACTIVE'
    const waitingForAdmin = inquiry.status === 'LIVE_REQUESTED'
    const waitingForRequester = inquiry.status === 'LIVE_OFFERED'
    const canStartLive = ['WAITING', 'IN_PROGRESS', 'ANSWERED'].includes(inquiry.status)
    const canWriteMessage = !waitingForAdmin && !waitingForRequester
    const messagesEndRef = useRef<HTMLDivElement>(null)
    const messageInputRef = useRef<HTMLTextAreaElement>(null)
    const resolutionInputRef = useRef<HTMLTextAreaElement>(null)

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ block: 'end' })
    }, [detail.messages])

    useEffect(() => {
        resizeTextArea(messageInputRef.current)
    }, [props.message])

    useEffect(() => {
        resizeTextArea(resolutionInputRef.current)
    }, [props.resolution])

    const selectAttachments = (files: FileList | null) => {
        if (!files) return
        const selected = [...files]
        if (props.attachments.length + selected.length > MAX_ATTACHMENTS) {
            window.alert(`첨부파일은 최대 ${MAX_ATTACHMENTS}개까지 선택할 수 있습니다.`)
            return
        }
        const invalid = selected.find((file) => (
            !SUPPORT_ATTACHMENT_TYPES.has(file.type) || file.size > MAX_ATTACHMENT_SIZE
        ))
        const totalSize = [...props.attachments, ...selected]
            .reduce((sum, file) => sum + file.size, 0)
        if (invalid || totalSize > MAX_ATTACHMENT_SIZE) {
            window.alert('JPG, PNG, WebP, PDF 파일만 첨부할 수 있으며 전체 용량은 최대 10MB입니다.')
            return
        }
        props.onAttachmentsChange([...props.attachments, ...selected])
    }

    return (
        <div className="flex h-full min-h-0 flex-col">
            <header className="shrink-0 border-b border-line p-4 min-[601px]:p-5">
                <div className="flex flex-wrap items-start justify-between gap-4">
                    <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                            <span className="text-xs font-extrabold text-accent">{CATEGORY_LABELS[inquiry.category]}</span>
                            <StatusBadge status={inquiry.status} />
                            {admin && <span className="text-xs text-muted">{inquiry.requesterName} · {inquiry.requesterType === 'SELLER' ? '판매자' : '일반 회원'}</span>}
                        </div>
                        <h2 className="mt-3 text-xl font-extrabold min-[601px]:text-2xl">{inquiry.title}</h2>
                        <p className="mt-2 text-xs text-muted">문의 #{inquiry.inquiryId} · {formatKoreanDateTime(inquiry.createdAt)}</p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                        {canStartLive && !admin && (
                            <ActionButton icon={Headphones} label="실시간 상담 요청" onClick={props.onRequestLive} disabled={props.submitting} />
                        )}
                        {canStartLive && admin && (
                            <ActionButton icon={Radio} label="실시간 상담 제안" onClick={props.onOfferLive} disabled={props.submitting} />
                        )}
                        {waitingForAdmin && admin && (
                            <>
                                <ActionButton icon={CheckCircle2} label="상담 수락" onClick={props.onAcceptLive} disabled={props.submitting} accent />
                                <ActionButton icon={X} label="거절" onClick={props.onRejectLive} disabled={props.submitting} />
                            </>
                        )}
                        {waitingForAdmin && !admin && (
                            <ActionButton icon={X} label="상담 요청 취소" onClick={props.onCancelLive} disabled={props.submitting} />
                        )}
                        {waitingForRequester && !admin && (
                            <>
                                <ActionButton icon={CheckCircle2} label="상담 수락" onClick={props.onAcceptLive} disabled={props.submitting} accent />
                                <ActionButton icon={X} label="거절" onClick={props.onRejectLive} disabled={props.submitting} />
                            </>
                        )}
                        {waitingForRequester && admin && (
                            <ActionButton icon={X} label="상담 제안 취소" onClick={props.onCancelLive} disabled={props.submitting} />
                        )}
                        {liveActive && (
                            <ActionButton icon={X} label="상담 종료" onClick={props.onEndLive} disabled={props.submitting} />
                        )}
                        {inquiry.status === 'CLOSED' && !admin && props.onCreateNew && (
                            <ActionButton icon={Plus} label="새 문의" onClick={props.onCreateNew} disabled={props.submitting} />
                        )}
                    </div>
                </div>
                {(waitingForAdmin || waitingForRequester || liveActive) && (
                    <div className={`mt-5 flex items-center gap-3 border px-4 py-3 text-sm ${liveActive ? 'border-lime bg-lime/15' : 'border-accent/40 bg-accent/5'}`}>
                        <CircleDot className={`size-4 ${liveActive ? 'animate-pulse text-lime' : 'text-accent'}`} aria-hidden="true" />
                        <span className="flex-1">
                            {liveActive
                                ? `${session?.adminName ?? '관리자'}와 실시간 상담 중입니다.`
                                : waitingForAdmin
                                    ? '관리자 수락을 기다리고 있습니다.'
                                    : '관리자의 실시간 상담 제안이 도착했습니다.'}
                        </span>
                        {liveActive && <strong className={props.connected ? 'text-success' : 'text-danger'}>{props.connected ? '연결됨' : '재연결 중'}</strong>}
                    </div>
                )}
            </header>

            <section className="min-h-0 flex-1 space-y-4 overflow-y-auto overscroll-contain bg-paper/45 p-5 min-[601px]:p-6" aria-label="문의 대화">
                {detail.messages.map((item) => {
                    if (item.type === 'SYSTEM') {
                        return <p key={item.messageId} className="mx-auto w-fit rounded-full bg-line/70 px-4 py-2 text-xs text-muted">{item.content}</p>
                    }
                    const mine = item.authorId === props.currentMemberId
                    return (
                        <article key={item.messageId} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                            <div className={`max-w-[85%] min-[601px]:max-w-[70%] ${mine ? 'text-right' : ''}`}>
                                <div className="mb-1.5 flex items-center gap-2 text-xs text-muted">
                                    {mine ? <span className="ml-auto">나</span> : <span>{item.authorRole === 'ROLE_ADMIN' ? 'YMall 관리자' : item.authorName}</span>}
                                    {item.type === 'RESOLUTION' && <strong className="text-accent">처리 결과</strong>}
                                </div>
                                {item.content && (
                                    <p className={`whitespace-pre-wrap rounded-2xl px-4 py-3 text-left text-sm leading-6 ${mine ? 'rounded-tr-sm bg-ink text-paper' : item.type === 'RESOLUTION' ? 'rounded-tl-sm border border-accent/35 bg-accent/8' : 'rounded-tl-sm border border-line bg-surface'}`}>
                                        {item.content}
                                    </p>
                                )}
                                {item.attachments.length > 0 && (
                                    <div className="mt-2 grid gap-2 text-left">
                                        {item.attachments.map((attachment) => (
                                            <SupportAttachmentItem
                                                key={attachment.attachmentId}
                                                attachment={attachment}
                                            />
                                        ))}
                                    </div>
                                )}
                                <time className="mt-1.5 block text-[10px] text-muted">{formatKoreanDateTime(item.createdAt)}</time>
                            </div>
                        </article>
                    )
                })}
                <div ref={messagesEndRef} />
            </section>

            {inquiry.status !== 'CLOSED' && canWriteMessage && (
                <form className="shrink-0 border-t border-line bg-surface p-4" onSubmit={props.onSubmitMessage}>
                    {props.attachments.length > 0 && (
                        <div className="mb-3 flex flex-wrap gap-2">
                            {props.attachments.map((file, index) => (
                                <span key={`${file.name}-${file.lastModified}`} className="inline-flex max-w-full items-center gap-2 border border-line bg-paper px-3 py-2 text-xs">
                                    {file.type.startsWith('image/') ? <ImageIcon className="size-4 shrink-0" /> : <FileText className="size-4 shrink-0" />}
                                    <span className="max-w-48 truncate">{file.name}</span>
                                    <button
                                        type="button"
                                        aria-label={`${file.name} 첨부 취소`}
                                        onClick={() => props.onAttachmentsChange(props.attachments.filter((_, itemIndex) => itemIndex !== index))}
                                    >
                                        <X className="size-3.5" />
                                    </button>
                                </span>
                            ))}
                        </div>
                    )}
                    <div className="flex items-end gap-2">
                        <label className="grid size-12 shrink-0 cursor-pointer place-items-center border border-line bg-paper hover:border-ink" aria-label="파일 첨부">
                            <Paperclip className="size-4" />
                            <input
                                className="sr-only"
                                type="file"
                                multiple
                                accept="image/jpeg,image/png,image/webp,application/pdf"
                                onChange={(event) => {
                                    selectAttachments(event.target.files)
                                    event.target.value = ''
                                }}
                            />
                        </label>
                        <textarea
                            ref={messageInputRef}
                            className="min-h-12 max-h-30 flex-1 resize-none overflow-y-auto border border-line bg-paper px-4 py-3 text-sm leading-6 outline-none [scrollbar-width:none] focus:border-ink [&::-webkit-scrollbar]:hidden"
                            rows={1}
                            value={props.message}
                            maxLength={2000}
                            placeholder={liveActive ? '실시간 메시지를 입력해 주세요' : admin ? '답변을 입력해 주세요' : '추가 문의를 입력해 주세요'}
                            onChange={(event) => props.onMessageChange(event.target.value)}
                            onKeyDown={(event) => {
                                if (
                                    event.key === 'Enter'
                                    && !event.shiftKey
                                    && !event.nativeEvent.isComposing
                                ) {
                                    event.preventDefault()
                                    event.currentTarget.form?.requestSubmit()
                                }
                            }}
                        />
                        <button className="grid size-12 shrink-0 place-items-center bg-ink text-paper disabled:opacity-40" type="submit" aria-label="메시지 전송" disabled={props.submitting || (!props.message.trim() && props.attachments.length === 0) || (liveActive && props.attachments.length === 0 && !props.connected)}>
                            <Send className="size-4" />
                        </button>
                    </div>
                </form>
            )}

            {admin && inquiry.status !== 'CLOSED' && !liveActive && (
                <div className="shrink-0 border-t border-line bg-surface p-4">
                    <p className="mb-2 text-xs font-extrabold tracking-[.12em] text-muted">RESOLUTION</p>
                    <div className="flex items-end gap-2">
                        <textarea ref={resolutionInputRef} className="min-h-12 max-h-30 flex-1 resize-none overflow-y-auto border border-line bg-paper px-4 py-3 text-sm leading-6 outline-none [scrollbar-width:none] focus:border-ink [&::-webkit-scrollbar]:hidden" rows={1} value={props.resolution} maxLength={2000} placeholder="처리 결과를 작성하고 문의를 완료하세요" onChange={(event) => props.onResolutionChange(event.target.value)} />
                        <button className="inline-flex h-12 shrink-0 items-center gap-2 bg-lime px-4 text-sm font-extrabold text-[#171717] disabled:opacity-40" type="button" disabled={props.submitting || !props.resolution.trim()} onClick={props.onClose}>
                            <UserRoundCheck className="size-4" /> 처리 완료
                        </button>
                    </div>
                </div>
            )}
        </div>
    )
}

function SupportAttachmentItem({ attachment }: { attachment: SupportAttachment }) {
    const [objectUrl, setObjectUrl] = useState<string | null>(null)
    const isImage = attachment.contentType.startsWith('image/')

    useEffect(() => {
        if (!isImage) return
        const controller = new AbortController()
        void fetch(attachment.downloadUrl, {
            headers: { Authorization: `Bearer ${getAccessToken() ?? ''}` },
            signal: controller.signal,
        }).then((response) => {
            if (!response.ok) throw new Error('첨부 이미지를 불러오지 못했습니다.')
            return response.blob()
        }).then((blob) => {
            setObjectUrl(URL.createObjectURL(blob))
        }).catch((error: unknown) => {
            if (!(error instanceof Error) || error.name !== 'AbortError') setObjectUrl(null)
        })
        return () => controller.abort()
    }, [attachment.downloadUrl, isImage])

    useEffect(() => () => {
        if (objectUrl) URL.revokeObjectURL(objectUrl)
    }, [objectUrl])

    const openAttachment = async () => {
        const response = await fetch(attachment.downloadUrl, {
            headers: { Authorization: `Bearer ${getAccessToken() ?? ''}` },
        })
        if (!response.ok) return
        const url = URL.createObjectURL(await response.blob())
        window.open(url, '_blank', 'noopener,noreferrer')
        window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
    }

    return (
        <button type="button" className="overflow-hidden border border-line bg-surface text-left" onClick={() => void openAttachment()}>
            {isImage && objectUrl && <img className="max-h-72 w-full object-contain" src={objectUrl} alt={attachment.fileName} />}
            <span className="flex items-center gap-2 px-3 py-2 text-xs">
                {isImage ? <ImageIcon className="size-4 shrink-0" /> : <FileText className="size-4 shrink-0" />}
                <span className="min-w-0 flex-1 truncate">{attachment.fileName}</span>
                <span className="text-muted">{formatFileSize(attachment.fileSize)}</span>
            </span>
        </button>
    )
}

function formatFileSize(bytes: number) {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${Math.ceil(bytes / 1024)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function resizeTextArea(textArea: HTMLTextAreaElement | null) {
    if (!textArea) return
    textArea.style.height = '48px'
    textArea.style.height = `${Math.min(textArea.scrollHeight, 120)}px`
}

function StatusBadge({ status }: { status: SupportInquiryStatus }) {
    const tone: StatusBadgeTone = status === 'LIVE_ACTIVE'
        ? 'success'
        : status === 'CLOSED'
            ? 'neutral'
            : status === 'WAITING' || status === 'LIVE_REQUESTED'
                ? 'warning'
                : 'info'
    return <CommonStatusBadge tone={tone}>{STATUS_LABELS[status]}</CommonStatusBadge>
}

function ActionButton({ icon: Icon, label, onClick, disabled, accent = false }: {
    icon: typeof MessageCircleMore
    label: string
    onClick: () => void
    disabled: boolean
    accent?: boolean
}) {
    return (
        <button className={`inline-flex items-center gap-2 border px-3 py-2 text-xs font-bold disabled:opacity-40 ${accent ? 'border-lime bg-lime text-[#171717]' : 'border-line bg-surface'}`} type="button" disabled={disabled} onClick={onClick}>
            <Icon className="size-3.5" aria-hidden="true" /> {label}
        </button>
    )
}
