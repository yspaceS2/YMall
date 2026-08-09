import { LockKeyhole, Pencil, Trash2 } from 'lucide-react'
import type { ProductQuestion } from '../../types/productQuestion'
import { formatKoreanDateTime } from '../../utils/dateTime'
import { PageState } from '../ui/PageState'
import { StatusBadge } from '../ui/StatusBadge'

export function ProductQuestionList({
    questions,
    isLoading,
    onEdit,
    onDelete,
}: {
    questions: ProductQuestion[]
    isLoading: boolean
    onEdit: (question: ProductQuestion) => void
    onDelete: (question: ProductQuestion) => void
}) {
    if (isLoading) {
        return <PageState variant="loading" title="상품 문의를 불러오는 중입니다" compact />
    }
    if (questions.length === 0) {
        return (
            <PageState
                variant="empty"
                title="등록된 상품 문의가 없습니다"
                description="상품에 대해 궁금한 점을 판매자에게 문의해 보세요."
                compact
            />
        )
    }
    return (
        <div className="border-t-2 border-ink">
            {questions.map((question) => (
                <article className="border-b border-line py-6" key={question.questionId}>
                    <div className="flex flex-wrap items-center gap-2 text-[11px] text-muted">
                        <StatusBadge tone={question.status === 'ANSWERED' ? 'success' : 'warning'}>
                            {question.status === 'ANSWERED' ? '답변 완료' : '답변 대기'}
                        </StatusBadge>
                        {question.privateQuestion && <span className="inline-flex items-center gap-1"><LockKeyhole className="size-3" /> 비밀글</span>}
                        <span>{question.memberName}</span>
                        <time>{formatKoreanDateTime(question.createdAt)}</time>
                    </div>
                    <div className="mt-3 flex items-start justify-between gap-4">
                        <div>
                            <h3 className="text-sm font-bold">{question.title}</h3>
                            {question.contentVisible && question.content && <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-muted">{question.content}</p>}
                        </div>
                        {question.ownedByRequester && (
                            <div className="flex shrink-0 gap-1">
                                {question.status === 'WAITING' && <button className="grid size-9 place-items-center" aria-label="문의 수정" type="button" onClick={() => onEdit(question)}><Pencil className="size-4" /></button>}
                                <button className="grid size-9 place-items-center text-danger" aria-label="문의 삭제" type="button" onClick={() => onDelete(question)}><Trash2 className="size-4" /></button>
                            </div>
                        )}
                    </div>
                    {question.answer && (
                        <div className="mt-5 ml-5 border-l-2 border-lime bg-paper p-5">
                            <strong className="text-xs">판매자 답변</strong>
                            <p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-muted">{question.answer.content}</p>
                            <time className="mt-3 block text-[11px] text-muted">{formatKoreanDateTime(question.answer.updatedAt)}</time>
                        </div>
                    )}
                </article>
            ))}
        </div>
    )
}
