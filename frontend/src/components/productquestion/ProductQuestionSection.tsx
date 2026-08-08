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
import { ConfirmDialog } from '../ui/ConfirmDialog'
import { FeedbackMessage } from '../ui/FeedbackMessage'
import { ProductQuestionForm } from './ProductQuestionForm'
import { ProductQuestionList } from './ProductQuestionList'

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
                <ProductQuestionForm
                    form={form}
                    isEditing={editingQuestionId !== null}
                    isSaving={isSaving}
                    onChange={setForm}
                    onCancel={() => setIsFormOpen(false)}
                    onSubmit={submitQuestion}
                />
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

            <ProductQuestionList
                questions={questions}
                isLoading={isLoading}
                onEdit={openEditForm}
                onDelete={setQuestionToDelete}
            />

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
