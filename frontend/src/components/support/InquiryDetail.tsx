import {
    CheckCircle2,
    CircleDot,
    Headphones,
    MessageCircleMore,
    Plus,
    Radio,
    X,
} from 'lucide-react'
import type { FormEvent } from 'react'
import { CATEGORY_LABELS, STATUS_LABELS } from '../../pages/supportPresentation'
import type {
    SupportInquiryDetail,
    SupportInquiryStatus,
} from '../../types/support'
import { formatKoreanDateTime } from '../../utils/dateTime'
import { StatusBadge as CommonStatusBadge, type StatusBadgeTone } from '../ui/StatusBadge'
import { InquiryComposer } from './InquiryComposer'
import { InquiryConversation } from './InquiryConversation'

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
    const { inquiry, chatSession } = props.detail
    const liveActive = inquiry.status === 'LIVE_ACTIVE'
    const waitingForAdmin = inquiry.status === 'LIVE_REQUESTED'
    const waitingForRequester = inquiry.status === 'LIVE_OFFERED'
    const canStartLive = ['WAITING', 'IN_PROGRESS', 'ANSWERED'].includes(inquiry.status)

    return (
        <div className="flex h-full min-h-0 flex-col">
            <header className="shrink-0 border-b border-line p-4 min-[601px]:p-5">
                <div className="flex flex-wrap items-start justify-between gap-4">
                    <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                            <span className="text-xs font-extrabold text-accent">
                                {CATEGORY_LABELS[inquiry.category]}
                            </span>
                            <SupportStatusBadge status={inquiry.status} />
                            {props.admin && (
                                <span className="text-xs text-muted">
                                    {inquiry.requesterName} · {
                                        inquiry.requesterType === 'SELLER' ? '판매자' : '일반 회원'
                                    }
                                </span>
                            )}
                        </div>
                        <h2 className="mt-3 text-xl font-extrabold min-[601px]:text-2xl">
                            {inquiry.title}
                        </h2>
                        <p className="mt-2 text-xs text-muted">
                            문의 #{inquiry.inquiryId} · {formatKoreanDateTime(inquiry.createdAt)}
                        </p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                        {canStartLive && !props.admin && (
                            <ActionButton
                                icon={Headphones}
                                label="실시간 상담 요청"
                                onClick={props.onRequestLive}
                                disabled={props.submitting}
                            />
                        )}
                        {canStartLive && props.admin && (
                            <ActionButton
                                icon={Radio}
                                label="실시간 상담 제안"
                                onClick={props.onOfferLive}
                                disabled={props.submitting}
                            />
                        )}
                        {waitingForAdmin && props.admin && (
                            <>
                                <ActionButton
                                    accent
                                    icon={CheckCircle2}
                                    label="상담 수락"
                                    onClick={props.onAcceptLive}
                                    disabled={props.submitting}
                                />
                                <ActionButton
                                    icon={X}
                                    label="거절"
                                    onClick={props.onRejectLive}
                                    disabled={props.submitting}
                                />
                            </>
                        )}
                        {waitingForAdmin && !props.admin && (
                            <ActionButton
                                icon={X}
                                label="상담 요청 취소"
                                onClick={props.onCancelLive}
                                disabled={props.submitting}
                            />
                        )}
                        {waitingForRequester && !props.admin && (
                            <>
                                <ActionButton
                                    accent
                                    icon={CheckCircle2}
                                    label="상담 수락"
                                    onClick={props.onAcceptLive}
                                    disabled={props.submitting}
                                />
                                <ActionButton
                                    icon={X}
                                    label="거절"
                                    onClick={props.onRejectLive}
                                    disabled={props.submitting}
                                />
                            </>
                        )}
                        {waitingForRequester && props.admin && (
                            <ActionButton
                                icon={X}
                                label="상담 제안 취소"
                                onClick={props.onCancelLive}
                                disabled={props.submitting}
                            />
                        )}
                        {liveActive && (
                            <ActionButton
                                icon={X}
                                label="상담 종료"
                                onClick={props.onEndLive}
                                disabled={props.submitting}
                            />
                        )}
                        {inquiry.status === 'CLOSED' && !props.admin && props.onCreateNew && (
                            <ActionButton
                                icon={Plus}
                                label="새 문의"
                                onClick={props.onCreateNew}
                                disabled={props.submitting}
                            />
                        )}
                    </div>
                </div>
                {(waitingForAdmin || waitingForRequester || liveActive) && (
                    <div className={`mt-5 flex items-center gap-3 border px-4 py-3 text-sm ${
                        liveActive
                            ? 'border-lime bg-lime/15'
                            : 'border-accent/40 bg-accent/5'
                    }`}>
                        <CircleDot
                            aria-hidden="true"
                            className={`size-4 ${
                                liveActive ? 'animate-pulse text-lime' : 'text-accent'
                            }`}
                        />
                        <span className="flex-1">
                            {liveActive
                                ? `${chatSession?.adminName ?? '관리자'}와 실시간 상담 중입니다.`
                                : waitingForAdmin
                                    ? '관리자 수락을 기다리고 있습니다.'
                                    : '관리자의 실시간 상담 제안이 도착했습니다.'}
                        </span>
                        {liveActive && (
                            <strong className={props.connected ? 'text-success' : 'text-danger'}>
                                {props.connected ? '연결됨' : '재연결 중'}
                            </strong>
                        )}
                    </div>
                )}
            </header>

            <InquiryConversation
                currentMemberId={props.currentMemberId}
                messages={props.detail.messages}
            />
            <InquiryComposer
                admin={props.admin}
                attachments={props.attachments}
                connected={props.connected}
                message={props.message}
                resolution={props.resolution}
                status={inquiry.status}
                submitting={props.submitting}
                onAttachmentsChange={props.onAttachmentsChange}
                onClose={props.onClose}
                onMessageChange={props.onMessageChange}
                onResolutionChange={props.onResolutionChange}
                onSubmitMessage={props.onSubmitMessage}
            />
        </div>
    )
}

function SupportStatusBadge({ status }: { status: SupportInquiryStatus }) {
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
        <button
            className={`inline-flex items-center gap-2 border px-3 py-2 text-xs font-bold disabled:opacity-40 ${
                accent
                    ? 'border-lime bg-lime text-[#171717]'
                    : 'border-line bg-surface'
            }`}
            disabled={disabled}
            type="button"
            onClick={onClick}
        >
            <Icon aria-hidden="true" className="size-3.5" /> {label}
        </button>
    )
}
