import { ArrowLeft } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import {
    getSellerProductQuestion,
    notifySellerQuestionCountChanged,
    saveSellerProductQuestionAnswer,
} from '../api/productQuestions'
import {
    ManagementPageHeader,
    managementPageClassName,
} from '../components/management/ManagementListUi'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { PageState } from '../components/ui/PageState'
import type { ProductQuestion } from '../types/productQuestion'
import { formatKoreanDateTime } from '../utils/dateTime'
import { ProductQuestionThumbnail } from './SellerProductQuestionUi'

export function SellerProductQuestionDetailPage() {
    const { questionId } = useParams()
    const parsedQuestionId = Number(questionId)
    const validQuestionId = Number.isInteger(parsedQuestionId) && parsedQuestionId > 0
    const [question, setQuestion] = useState<ProductQuestion | null>(null)
    const [answer, setAnswer] = useState('')
    const [isLoading, setIsLoading] = useState(validQuestionId)
    const [isSaving, setIsSaving] = useState(false)
    const [errorMessage, setErrorMessage] = useState('')
    const [successMessage, setSuccessMessage] = useState('')

    useEffect(() => {
        if (!validQuestionId) return
        const controller = new AbortController()
        getSellerProductQuestion(parsedQuestionId, controller.signal)
            .then((response) => {
                setQuestion(response)
                setAnswer(response.answer?.content ?? '')
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
        return () => controller.abort()
    }, [parsedQuestionId, validQuestionId])

    async function saveAnswer(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        if (!question || !answer.trim()) return
        setIsSaving(true)
        setSuccessMessage('')
        try {
            const updated = await saveSellerProductQuestionAnswer(
                question.questionId,
                answer.trim(),
            )
            setQuestion(updated)
            setAnswer(updated.answer?.content ?? '')
            setErrorMessage('')
            setSuccessMessage('상품 문의 답변이 저장되었습니다.')
            notifySellerQuestionCountChanged()
        } catch (error) {
            setErrorMessage(
                error instanceof ApiError
                    ? error.message
                    : '상품 문의 답변을 저장하지 못했습니다.',
            )
        } finally {
            setIsSaving(false)
        }
    }

    if (!validQuestionId) {
        return (
            <PageState
                variant="error"
                title="상품 문의를 찾을 수 없습니다"
                description="잘못된 상품 문의 주소입니다."
            />
        )
    }
    if (isLoading) {
        return <PageState variant="loading" title="상품 문의를 불러오는 중입니다" />
    }

    return (
        <section className={managementPageClassName}>
            <Link
                className="mb-6 inline-flex items-center gap-2 text-xs font-bold text-muted hover:text-ink"
                to="/seller/questions"
            >
                <ArrowLeft className="size-4" /> 상품 문의 목록
            </Link>
            <ManagementPageHeader
                eyebrow="PRODUCT QUESTION DETAIL"
                title="상품 문의 상세"
                description="구매자의 문의 내용을 확인하고 답변합니다."
                action={question ? (
                    <Link
                        className="h-11 border border-ink bg-surface px-5 py-3 text-xs font-bold"
                        to={`/products/${question.productId}`}
                    >
                        상품 페이지에서 보기
                    </Link>
                ) : undefined}
            />
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
            {question && (
                <div className="grid gap-6">
                    <section className="border border-line bg-paper p-6">
                        <div className="flex flex-wrap items-center gap-4">
                            <ProductQuestionThumbnail name={question.productName} url={question.thumbnailUrl} />
                            <div>
                                <p className="text-xs text-muted">{question.productName}</p>
                                <h2 className="mt-1 text-lg font-bold">{question.title}</h2>
                            </div>
                        </div>
                        <dl className="mt-6 grid gap-4 border-t border-line pt-5 text-sm md:grid-cols-3">
                            <Detail label="문의자" value={question.memberName} />
                            <Detail
                                label="공개 여부"
                                value={question.privateQuestion ? '비공개' : '공개'}
                            />
                            <Detail
                                label="문의일"
                                value={formatKoreanDateTime(question.createdAt)}
                            />
                        </dl>
                        <div className="mt-6 border-t border-line pt-5">
                            <p className="text-xs font-bold text-muted">문의 내용</p>
                            <p className="mt-3 whitespace-pre-wrap text-sm leading-7">
                                {question.content}
                            </p>
                        </div>
                    </section>
                    <form className="border border-line bg-paper p-6" onSubmit={saveAnswer}>
                        <label className="grid gap-3 text-sm font-bold">
                            판매자 답변
                            <textarea
                                className="min-h-44 resize-y border border-line bg-surface p-4 text-sm font-normal leading-7 outline-0 focus:border-ink"
                                maxLength={2000}
                                placeholder="구매자가 이해하기 쉽도록 답변을 작성해 주세요."
                                value={answer}
                                onChange={(event) => setAnswer(event.target.value)}
                                required
                            />
                        </label>
                        <div className="mt-4 flex justify-end">
                            <button
                                className="h-11 bg-ink px-6 text-xs font-bold text-white disabled:opacity-50"
                                disabled={isSaving}
                                type="submit"
                            >
                                {isSaving
                                    ? '저장 중...'
                                    : question.status === 'ANSWERED'
                                        ? '답변 수정'
                                        : '답변 등록'}
                            </button>
                        </div>
                    </form>
                </div>
            )}
        </section>
    )
}

function Detail({ label, value }: { label: string; value: string }) {
    return (
        <div>
            <dt className="text-xs font-bold text-muted">{label}</dt>
            <dd className="mt-1.5">{value}</dd>
        </div>
    )
}
