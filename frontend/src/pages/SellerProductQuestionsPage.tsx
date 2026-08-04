import { LockKeyhole } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { getSellerProductQuestions } from '../api/productQuestions'
import {
    ManagementEmpty,
    ManagementListSearch,
    ManagementPageHeader,
    ManagementPagination,
    managementPageClassName,
} from '../components/management/ManagementListUi'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import { PageState } from '../components/ui/PageState'
import type {
    ProductQuestion,
    ProductQuestionStatus,
} from '../types/productQuestion'
import { formatKoreanDateTime } from '../utils/dateTime'
import {
    ProductQuestionStatusBadge,
    ProductQuestionThumbnail,
} from './SellerProductQuestionUi'

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
                                            <ProductQuestionThumbnail
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
                                        <ProductQuestionStatusBadge status={question.status} />
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

function parseStatus(value: string | null): ProductQuestionStatus | undefined {
    return value === 'WAITING' || value === 'ANSWERED' ? value : undefined
}
