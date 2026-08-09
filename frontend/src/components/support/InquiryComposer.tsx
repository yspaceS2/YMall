import {
    FileText,
    Image as ImageIcon,
    Paperclip,
    Send,
    UserRoundCheck,
    X,
} from 'lucide-react'
import { useEffect, useRef, type FormEvent } from 'react'
import type { SupportInquiryStatus } from '../../types/support'

const SUPPORT_ATTACHMENT_TYPES = new Set([
    'image/jpeg', 'image/png', 'image/webp', 'application/pdf',
])
const MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024
const MAX_ATTACHMENTS = 5

interface InquiryComposerProps {
    admin: boolean
    connected: boolean
    status: SupportInquiryStatus
    message: string
    attachments: File[]
    resolution: string
    submitting: boolean
    onMessageChange: (value: string) => void
    onAttachmentsChange: (files: File[]) => void
    onResolutionChange: (value: string) => void
    onSubmitMessage: (event: FormEvent) => void
    onClose: () => void
}

export function InquiryComposer(props: InquiryComposerProps) {
    const messageInputRef = useRef<HTMLTextAreaElement>(null)
    const resolutionInputRef = useRef<HTMLTextAreaElement>(null)
    const liveActive = props.status === 'LIVE_ACTIVE'
    const waitingForResponse = props.status === 'LIVE_REQUESTED'
        || props.status === 'LIVE_OFFERED'
    const canWriteMessage = !waitingForResponse

    useEffect(() => {
        resizeTextArea(messageInputRef.current)
    }, [props.message])

    useEffect(() => {
        resizeTextArea(resolutionInputRef.current)
    }, [props.resolution])

    function selectAttachments(files: FileList | null) {
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
        <>
            {props.status !== 'CLOSED' && canWriteMessage && (
                <form
                    className="shrink-0 border-t border-line bg-surface p-4"
                    onSubmit={props.onSubmitMessage}
                >
                    {props.attachments.length > 0 && (
                        <div className="mb-3 flex flex-wrap gap-2">
                            {props.attachments.map((file, index) => (
                                <span
                                    className="inline-flex max-w-full items-center gap-2 border border-line bg-paper px-3 py-2 text-xs"
                                    key={`${file.name}-${file.lastModified}`}
                                >
                                    {file.type.startsWith('image/')
                                        ? <ImageIcon className="size-4 shrink-0" />
                                        : <FileText className="size-4 shrink-0" />}
                                    <span className="max-w-48 truncate">{file.name}</span>
                                    <button
                                        aria-label={`${file.name} 첨부 취소`}
                                        type="button"
                                        onClick={() => props.onAttachmentsChange(
                                            props.attachments.filter((_, itemIndex) => itemIndex !== index),
                                        )}
                                    >
                                        <X className="size-3.5" />
                                    </button>
                                </span>
                            ))}
                        </div>
                    )}
                    <div className="flex items-end gap-2">
                        <label
                            aria-label="파일 첨부"
                            className="grid size-12 shrink-0 cursor-pointer place-items-center border border-line bg-paper hover:border-ink"
                        >
                            <Paperclip className="size-4" />
                            <input
                                accept="image/jpeg,image/png,image/webp,application/pdf"
                                className="sr-only"
                                multiple
                                type="file"
                                onChange={(event) => {
                                    selectAttachments(event.target.files)
                                    event.target.value = ''
                                }}
                            />
                        </label>
                        <textarea
                            className="min-h-12 max-h-30 flex-1 resize-none overflow-y-auto border border-line bg-paper px-4 py-3 text-sm leading-6 outline-none [scrollbar-width:none] focus:border-ink [&::-webkit-scrollbar]:hidden"
                            maxLength={2000}
                            placeholder={liveActive
                                ? '실시간 메시지를 입력해 주세요'
                                : props.admin ? '답변을 입력해 주세요' : '추가 문의를 입력해 주세요'}
                            ref={messageInputRef}
                            rows={1}
                            value={props.message}
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
                        <button
                            aria-label="메시지 전송"
                            className="grid size-12 shrink-0 place-items-center bg-ink text-paper disabled:opacity-40"
                            disabled={
                                props.submitting
                                || (!props.message.trim() && props.attachments.length === 0)
                                || (liveActive && props.attachments.length === 0 && !props.connected)
                            }
                            type="submit"
                        >
                            <Send className="size-4" />
                        </button>
                    </div>
                </form>
            )}

            {props.admin && props.status !== 'CLOSED' && !liveActive && (
                <div className="shrink-0 border-t border-line bg-surface p-4">
                    <p className="mb-2 text-xs font-extrabold tracking-[.12em] text-muted">
                        RESOLUTION
                    </p>
                    <div className="flex items-end gap-2">
                        <textarea
                            className="min-h-12 max-h-30 flex-1 resize-none overflow-y-auto border border-line bg-paper px-4 py-3 text-sm leading-6 outline-none [scrollbar-width:none] focus:border-ink [&::-webkit-scrollbar]:hidden"
                            maxLength={2000}
                            placeholder="처리 결과를 작성하고 문의를 완료하세요"
                            ref={resolutionInputRef}
                            rows={1}
                            value={props.resolution}
                            onChange={(event) => props.onResolutionChange(event.target.value)}
                        />
                        <button
                            className="inline-flex h-12 shrink-0 items-center gap-2 bg-lime px-4 text-sm font-extrabold text-[#171717] disabled:opacity-40"
                            disabled={props.submitting || !props.resolution.trim()}
                            type="button"
                            onClick={props.onClose}
                        >
                            <UserRoundCheck className="size-4" /> 처리 완료
                        </button>
                    </div>
                </div>
            )}
        </>
    )
}

function resizeTextArea(textArea: HTMLTextAreaElement | null) {
    if (!textArea) return
    textArea.style.height = '48px'
    textArea.style.height = `${Math.min(textArea.scrollHeight, 120)}px`
}
