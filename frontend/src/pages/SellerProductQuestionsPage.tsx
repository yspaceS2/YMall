import { ArrowLeft, LockKeyhole } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import {
    getSellerProductQuestion,
    getSellerProductQuestions,
    notifySellerQuestionCountChanged,
    saveSellerProductQuestionAnswer,
} from '../api/productQuestions'
import {
    ManagementEmpty,
    ManagementListSearch,
    ManagementPageHeader,
    ManagementPagination,
    managementPageClassName,
} from '../components/management/ManagementListUi'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { PageState } from '../components/ui/PageState'
import { StatusBadge } from '../components/ui/StatusBadge'
import type {
    ProductQuestion,
    ProductQuestionStatus,
} from '../types/productQuestion'
import { formatKoreanDateTime } from '../utils/dateTime'
import { resolveImageUrl } from '../utils/product'

const PAGE_SIZE = 20

export function SellerProductQuestionListPage() {
    const navigate = useNavigate()
    const [searchParams, setSearchParams] = useSearchParams()
    const page = Math.max(Number(searchParams.get('page')) || 1, 1)
    const keyword = searchParams.get('keyword') ?? ''
    const status = parseStatus(searchParams.get('status'))
    const [questions, setQuestions] = useState<ProductQuestion[]>([])
    const [pagination, setPagination] = useState({
        page: 1,
        totalPages: 0,
        totalElements: 0,
    })
    const [isLoading, setIsLoading] = useState(true)
    const [errorMessage, setErrorMessage] = useState('')

    useEffect(() => {
        const controller = new AbortController()
        getSellerProductQuestions({
            page,
            size: PAGE_SIZE,
            status,
            keyword,
            signal: controller.signal,
        })
            .then((response) => {
                setQuestions(response.content)
                setPagination({
                    page: response.page,
                    totalPages: response.totalPages,
                    totalElements: response.totalElements,
                })
                setErrorMessage('')
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(
                    error instanceof ApiError
                        ? error.message
                        : '상품 문의 목록을 불러오지 못했습니다.',
                )
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [keyword, page, status])

    function changeStatus(value: string) {
        const next = new URLSearchParams(searchParams)
        if (value) next.set('status', value)
        else next.delete('status')
        next.set('page', '1')
        setIsLoading(true)
        setSearchParams(next)
    }

    return (
        <section className={managementPageClassName}>
            <ManagementPageHeader
                eyebrow="PRODUCT QUESTIONS"
                title="상품 문의 관리"
                description={`전체 상품 문의 ${pagination.totalElements.toLocaleString()}건`}
            />
            <label className="mb-4 grid max-w-60 gap-1.5 text-xs font-bold">
                답변 상태
                <select
                    className="h-11 border border-line bg-surface px-3 text-sm font-normal text-ink outline-0 focus:border-ink"
                    value={status ?? ''}
                    onChange={(event) => changeStatus(event.target.value)}
                >
                    <option value="">전체</option>
                    <option value="WAITING">답변 대기</option>
                    <option value="ANSWERED">답변 완료</option>
                </select>
            </label>
            <ManagementListSearch placeholder="상품명, 문의 제목 또는 구매자명을 검색하세요" />
            {errorMessage && (
                <FeedbackMessage className="mb-5" tone="error">
                    {errorMessage}
                </FeedbackMessage>
            )}
            {isLoading ? (
                <PageState variant="loading" title="상품 문의를 불러오는 중입니다" compact />
            ) : questions.length === 0 ? (
                <ManagementEmpty>조건에 맞는 상품 문의가 없습니다.</ManagementEmpty>
            ) : (
                <div className="overflow-x-auto border-t-2 border-ink">
                    <table className="w-full min-w-220 text-left text-sm">
                        <thead className="border-b border-line bg-surface text-xs">
                            <tr>
                                <th className="p-4">상품</th>
                                <th className="p-4">문의</th>
                                <th className="p-4">구매자</th>
                                <th className="p-4">공개 여부</th>
                                <th className="p-4">상태</th>
                                <th className="p-4">문의일</th>
                            </tr>
                        </thead>
                        <tbody>
                            {questions.map((question) => (
                                <tr
                                    className="cursor-pointer border-b border-line bg-paper transition-colors hover:bg-surface focus-visible:bg-surface focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-ink"
                                    key={question.questionId}
                                    onClick={() => navigate(`/seller/questions/${question.questionId}`)}
                                    onKeyDown={(event) => {
                                        if (event.key === 'Enter' || event.key === ' ') {
                                            event.preventDefault()
                                            navigate(`/seller/questions/${question.questionId}`)
                                        }
                                    }}
                                    role="link"
                                    tabIndex={0}
                                >
                                    <td className="p-4">
                                        <div className="flex min-w-64 items-center gap-3">
                                            <Thumbnail
                                                name={question.productName}
                                                url={question.thumbnailUrl}
                                            />
                                            <strong className="min-w-0 truncate">
                                                {question.productName}
                                            </strong>
                                        </div>
                                    </td>
                                    <td className="max-w-80 p-4">
                                        <span className="block truncate">{question.title}</span>
                                    </td>
                                    <td className="p-4">{question.memberName}</td>
                                    <td className="p-4">
                                        {question.privateQuestion ? (
                                            <span className="inline-flex items-center gap-1">
                                                <LockKeyhole className="size-3.5" /> 비공개
                                            </span>
                                        ) : '공개'}
                                    </td>
                                    <td className="p-4">
                                        <QuestionStatusBadge status={question.status} />
                                    </td>
                                    <td className="p-4">
                                        {formatKoreanDateTime(question.createdAt)}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            <ManagementPagination
                page={pagination.page}
                totalPages={pagination.totalPages}
            />
        </section>
    )
}

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
                            <Thumbnail name={question.productName} url={question.thumbnailUrl} />
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

function Thumbnail({ name, url }: { name: string; url: string | null }) {
    return (
        <div className="size-14 shrink-0 overflow-hidden border border-line bg-surface">
            {url ? (
                <img
                    alt=""
                    className="size-full object-cover"
                    loading="lazy"
                    src={resolveImageUrl(url)}
                />
            ) : (
                <div className="grid size-full place-items-center text-[9px] font-bold tracking-[.12em] text-muted">
                    YMALL
                </div>
            )}
            <span className="sr-only">{name}</span>
        </div>
    )
}

function QuestionStatusBadge({ status }: { status: ProductQuestionStatus }) {
    return (
        <StatusBadge tone={status === 'ANSWERED' ? 'success' : 'warning'}>
            {status === 'ANSWERED' ? '답변 완료' : '답변 대기'}
        </StatusBadge>
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

function parseStatus(value: string | null): ProductQuestionStatus | undefined {
    return value === 'WAITING' || value === 'ANSWERED' ? value : undefined
}
