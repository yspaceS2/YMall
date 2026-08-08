import { useEffect, useRef } from 'react'
import type { SupportMessage } from '../../types/support'
import { formatKoreanDateTime } from '../../utils/dateTime'
import { SupportAttachmentItem } from './SupportAttachmentItem'

export function InquiryConversation({
    messages,
    currentMemberId,
}: {
    messages: SupportMessage[]
    currentMemberId: number | null
}) {
    const messagesEndRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ block: 'end' })
    }, [messages])

    return (
        <section
            aria-label="문의 대화"
            className="min-h-0 flex-1 space-y-4 overflow-y-auto overscroll-contain bg-paper/45 p-5 min-[601px]:p-6"
        >
            {messages.map((item) => {
                if (item.type === 'SYSTEM') {
                    return (
                        <p
                            className="mx-auto w-fit rounded-full bg-line/70 px-4 py-2 text-xs text-muted"
                            key={item.messageId}
                        >
                            {item.content}
                        </p>
                    )
                }
                const mine = item.authorId === currentMemberId
                return (
                    <article
                        className={`flex ${mine ? 'justify-end' : 'justify-start'}`}
                        key={item.messageId}
                    >
                        <div className={`max-w-[85%] min-[601px]:max-w-[70%] ${mine ? 'text-right' : ''}`}>
                            <div className="mb-1.5 flex items-center gap-2 text-xs text-muted">
                                {mine
                                    ? <span className="ml-auto">나</span>
                                    : <span>{item.authorRole === 'ROLE_ADMIN' ? 'YMall 관리자' : item.authorName}</span>}
                                {item.type === 'RESOLUTION' && (
                                    <strong className="text-accent">처리 결과</strong>
                                )}
                            </div>
                            {item.content && (
                                <p className={`whitespace-pre-wrap rounded-2xl px-4 py-3 text-left text-sm leading-6 ${
                                    mine
                                        ? 'rounded-tr-sm bg-ink text-paper'
                                        : item.type === 'RESOLUTION'
                                            ? 'rounded-tl-sm border border-accent/35 bg-accent/8'
                                            : 'rounded-tl-sm border border-line bg-surface'
                                }`}>
                                    {item.content}
                                </p>
                            )}
                            {item.attachments.length > 0 && (
                                <div className="mt-2 grid gap-2 text-left">
                                    {item.attachments.map((attachment) => (
                                        <SupportAttachmentItem
                                            attachment={attachment}
                                            key={attachment.attachmentId}
                                        />
                                    ))}
                                </div>
                            )}
                            <time className="mt-1.5 block text-[10px] text-muted">
                                {formatKoreanDateTime(item.createdAt)}
                            </time>
                        </div>
                    </article>
                )
            })}
            <div ref={messagesEndRef} />
        </section>
    )
}
