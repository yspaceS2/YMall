import { LockKeyhole, Pencil, Trash2 } from 'lucide-react'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ApiError } from '../../api/client'
import {
    createProductQuestion,
    deleteProductQuestion,
    getProductQuestions,
    updateProductQuestion,
} from '../../api/productQuestions'
import type {
    ProductQuestion,
    ProductQuestionRequest,
} from '../../types/productQuestion'
import { formatKoreanDateTime } from '../../utils/dateTime'
import { ConfirmDialog } from '../ui/ConfirmDialog'
import { FeedbackMessage } from '../ui/FeedbackMessage'
import { PageState } from '../ui/PageState'
import { StatusBadge } from '../ui/StatusBadge'

const emptyForm: ProductQuestionRequest = {
    title: '',
    content: '',
    privateQuestion: false,
}

export function ProductQuestionSection({
    productId,
    isAuthenticated,
    onLoginRequired,
    onSuccess,
}: {
    productId: number
    isAuthenticated: boolean
    onLoginRequired: () => void
    onSuccess: (message: string) => void
}) {
    const [questions, setQuestions] = useState<ProductQuestion[]>([])
    const [totalElements, setTotalElements] = useState(0)
    const [nextPage, setNextPage] = useState(2)
    const [hasNext, setHasNext] = useState(false)
    const [isLoading, setIsLoading] = useState(true)
    const [isLoadingMore, setIsLoadingMore] = useState(false)
    const [isSaving, setIsSaving] = useState(false)
    const [errorMessage, setErrorMessage] = useState('')
    const [successMessage, setSuccessMessage] = useState('')
    const [isFormOpen, setIsFormOpen] = useState(false)
    const [editingQuestionId, setEditingQuestionId] = useState<number | null>(null)
    const [questionToDelete, setQuestionToDelete] = useState<ProductQuestion | null>(null)
    const [form, setForm] = useState<ProductQuestionRequest>(emptyForm)
    const loadMoreControllerRef = useRef<AbortController | null>(null)

    useEffect(() => {
        const controller = new AbortController()
        getProductQuestions(productId, 1, 10, controller.signal)
            .then((response) => {
                setQuestions(response.content)
                setTotalElements(response.totalElements)
                setHasNext(response.hasNext)
                setNextPage(2)
                setErrorMessage('')
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(
                    error instanceof ApiError
                        ? error.message
                        : '상품 문의를 불러오지 못했습니다.',
                )
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => {
            controller.abort()
            loadMoreControllerRef.current?.abort()
        }
    }, [productId])

    function openCreateForm() {
        if (!isAuthenticated) {
            onLoginRequired()
            return
        }
        setEditingQuestionId(null)
        setForm(emptyForm)
        setIsFormOpen(true)
    }

    function openEditForm(question: ProductQuestion) {
        setEditingQuestionId(question.questionId)
        setForm({
            title: question.title,
            content: question.content ?? '',
            privateQuestion: question.privateQuestion,
        })
        setIsFormOpen(true)
    }

    async function submitQuestion(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        if (!form.title.trim() || !form.content.trim()) return
        setIsSaving(true)
        setErrorMessage('')
        setSuccessMessage('')
        try {
            const request = {
                ...form,
                title: form.title.trim(),
                content: form.content.trim(),
            }
            const saved = editingQuestionId === null
                ? await createProductQuestion(productId, request)
                : await updateProductQuestion(editingQuestionId, request)
            setQuestions((current) => editingQuestionId === null
                ? [saved, ...current]
                : current.map((question) =>
                    question.questionId === saved.questionId ? saved : question
                ))
            if (editingQuestionId === null) {
                setTotalElements((current) => current + 1)
            }
            setIsFormOpen(false)
            setEditingQuestionId(null)
            setForm(emptyForm)
            const successMessage = editingQuestionId === null
                ? '상품 문의가 등록되었습니다.'
                : '상품 문의가 수정되었습니다.'
            setSuccessMessage(successMessage)
            onSuccess(successMessage)
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError
                    ? error.message
                    : '상품 문의를 저장하지 못했습니다.',
            )
        } finally {
            setIsSaving(false)
        }
    }

    async function removeQuestion() {
        if (!questionToDelete) return
        setIsSaving(true)
        setSuccessMessage('')
        try {
            await deleteProductQuestion(questionToDelete.questionId)
            setQuestions((current) => current.filter(
                (question) => question.questionId !== questionToDelete.questionId,
            ))
            setTotalElements((current) => Math.max(0, current - 1))
            setQuestionToDelete(null)
            setSuccessMessage('상품 문의가 삭제되었습니다.')
            onSuccess('상품 문의가 삭제되었습니다.')
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError
                    ? error.message
                    : '상품 문의를 삭제하지 못했습니다.',
            )
        } finally {
            setIsSaving(false)
        }
    }

    async function loadMore() {
        if (!hasNext || isLoadingMore) return
        const controller = new AbortController()
        loadMoreControllerRef.current?.abort()
        loadMoreControllerRef.current = controller
        setIsLoadingMore(true)
        try {
            const response = await getProductQuestions(
                productId,
                nextPage,
                10,
                controller.signal,
            )
            setQuestions((current) => [...current, ...response.content])
            setHasNext(response.hasNext)
            setNextPage((current) => current + 1)
        } catch (error) {
            if (error instanceof Error && error.name === 'AbortError') return
            setErrorMessage(
                error instanceof ApiError
                    ? error.message
                    : '상품 문의를 더 불러오지 못했습니다.',
            )
        } finally {
            if (!controller.signal.aborted) setIsLoadingMore(false)
        }
    }

    return (
        <div className="pt-10">
            <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
                <div>
                    <p className="mb-2 text-[11px] font-extrabold tracking-[.18em] text-accent">
                        PRODUCT Q&amp;A
                    </p>
                    <h2 className="font-serif text-4xl tracking-tight">
                        상품 Q&amp;A <span className="text-lg text-muted">({totalElements})</span>
                    </h2>
                </div>
                <button
                    className="h-11 border border-ink bg-ink px-5 text-xs font-bold text-white"
                    type="button"
                    onClick={openCreateForm}
                >
                    상품 문의하기
                </button>
            </div>

            {isFormOpen && (
                <form className="mb-8 grid gap-4 border border-line bg-paper p-5" onSubmit={submitQuestion}>
                    <label className="grid gap-2 text-xs font-bold">
                        문의 제목
                        <input
                            className="h-11 border border-line bg-surface px-3 text-sm font-normal outline-0 focus:border-ink"
                            maxLength={100}
                            value={form.title}
                            onChange={(event) => setForm({ ...form, title: event.target.value })}
                            required
                        />
                    </label>
                    <label className="grid gap-2 text-xs font-bold">
                        문의 내용
                        <textarea
                            className="min-h-32 resize-y border border-line bg-surface p-3 text-sm font-normal leading-6 outline-0 focus:border-ink"
                            maxLength={2000}
                            value={form.content}
                            onChange={(event) => setForm({ ...form, content: event.target.value })}
                            required
                        />
                    </label>
                    <label className="flex items-center gap-2 text-xs font-bold">
                        <input
                            checked={form.privateQuestion}
                            type="checkbox"
                            onChange={(event) => setForm({
                                ...form,
                                privateQuestion: event.target.checked,
                            })}
                        />
                        비밀 문의로 등록
                    </label>
                    <div className="flex justify-end gap-2">
                        <button
                            className="h-10 border border-line px-4 text-xs font-bold"
                            type="button"
                            onClick={() => setIsFormOpen(false)}
                        >
                            취소
                        </button>
                        <button
                            className="h-10 bg-ink px-5 text-xs font-bold text-white disabled:opacity-50"
                            disabled={isSaving}
                            type="submit"
                        >
                            {isSaving ? '저장 중...' : editingQuestionId === null ? '문의 등록' : '문의 수정'}
                        </button>
                    </div>
                </form>
            )}

            {errorMessage && (
                <FeedbackMessage className="mb-5" tone="error">
                    {errorMessage}
                </FeedbackMessage>
            )}
            {successMessage && (
                <FeedbackMessage className="mb-5" tone="success">
                    {successMessage}
                </FeedbackMessage>
            )}

            {isLoading ? (
                <PageState variant="loading" title="상품 문의를 불러오는 중입니다" compact />
            ) : questions.length === 0 ? (
                <PageState
                    variant="empty"
                    title="등록된 상품 문의가 없습니다"
                    description="상품에 대해 궁금한 점을 판매자에게 문의해 보세요."
                    compact
                />
            ) : (
                <div className="border-t-2 border-ink">
                    {questions.map((question) => (
                        <article className="border-b border-line py-6" key={question.questionId}>
                            <div className="flex flex-wrap items-center gap-2 text-[11px] text-muted">
                                <StatusBadge tone={question.status === 'ANSWERED' ? 'success' : 'warning'}>
                                    {question.status === 'ANSWERED' ? '답변 완료' : '답변 대기'}
                                </StatusBadge>
                                {question.privateQuestion && (
                                    <span className="inline-flex items-center gap-1">
                                        <LockKeyhole className="size-3" /> 비밀글
                                    </span>
                                )}
                                <span>{question.memberName}</span>
                                <time>{formatKoreanDateTime(question.createdAt)}</time>
                            </div>
                            <div className="mt-3 flex items-start justify-between gap-4">
                                <div>
                                    <h3 className="text-sm font-bold">{question.title}</h3>
                                    {question.contentVisible && question.content && (
                                        <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-muted">
                                            {question.content}
                                        </p>
                                    )}
                                </div>
                                {question.ownedByRequester && (
                                    <div className="flex shrink-0 gap-1">
                                        {question.status === 'WAITING' && (
                                            <button
                                                className="grid size-9 place-items-center"
                                                aria-label="문의 수정"
                                                type="button"
                                                onClick={() => openEditForm(question)}
                                            >
                                                <Pencil className="size-4" />
                                            </button>
                                        )}
                                        <button
                                            className="grid size-9 place-items-center text-danger"
                                            aria-label="문의 삭제"
                                            type="button"
                                            onClick={() => setQuestionToDelete(question)}
                                        >
                                            <Trash2 className="size-4" />
                                        </button>
                                    </div>
                                )}
                            </div>
                            {question.answer && (
                                <div className="mt-5 ml-5 border-l-2 border-lime bg-paper p-5">
                                    <strong className="text-xs">판매자 답변</strong>
                                    <p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-muted">
                                        {question.answer.content}
                                    </p>
                                    <time className="mt-3 block text-[11px] text-muted">
                                        {formatKoreanDateTime(question.answer.updatedAt)}
                                    </time>
                                </div>
                            )}
                        </article>
                    ))}
                </div>
            )}

            {hasNext && (
                <button
                    className="mx-auto mt-8 block h-11 border border-ink bg-surface px-7 text-xs font-bold disabled:opacity-50"
                    disabled={isLoadingMore}
                    type="button"
                    onClick={() => void loadMore()}
                >
                    {isLoadingMore ? '불러오는 중...' : '문의 더 보기'}
                </button>
            )}

            <ConfirmDialog
                open={questionToDelete !== null}
                title="상품 문의를 삭제할까요?"
                description="삭제한 문의와 판매자 답변은 복구할 수 없습니다."
                confirmLabel="문의 삭제"
                isPending={isSaving}
                onCancel={() => setQuestionToDelete(null)}
                onConfirm={() => void removeQuestion()}
            />
        </div>
    )
}
