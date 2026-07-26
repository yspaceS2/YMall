import {
    CircleAlert,
    MessageCircleMore,
    RefreshCw,
    Sparkles,
    ThumbsDown,
    ThumbsUp,
} from 'lucide-react'
import type { ReactNode } from 'react'
import type { ReviewSummary } from '../../types/review'
import { formatKoreanDateTime } from '../../utils/dateTime'

interface ReviewSummaryPanelProps {
    summary: ReviewSummary | null
    isLoading: boolean
    error: string
    onRetry: () => void
}

interface SummaryColumnProps {
    icon: ReactNode
    title: string
    items: string[]
    emptyMessage: string
}

function formatGeneratedAt(generatedAt: string | null) {
    return generatedAt ? formatKoreanDateTime(generatedAt) : ''
}

function SummaryColumn({ icon, title, items, emptyMessage }: SummaryColumnProps) {
    return (
        <section className="border-t border-[#d9dbca] pt-5 min-[801px]:border-t-0 min-[801px]:border-l min-[801px]:pt-0 min-[801px]:pl-6 first:border-t-0 first:pt-0 min-[801px]:first:border-l-0 min-[801px]:first:pl-0">
            <h4 className="mb-4 flex items-center gap-2 text-xs font-extrabold tracking-[.06em]">
                {icon}
                {title}
            </h4>
            {items.length > 0 ? (
                <ul className="space-y-3">
                    {items.map((item, index) => (
                        <li
                            className="flex gap-2.5 text-sm leading-6 text-[#56574f]"
                            key={`${title}-${index}`}
                        >
                            <span className="mt-2 size-1.5 shrink-0 rounded-full bg-[#8ba127]" />
                            <span>{item}</span>
                        </li>
                    ))}
                </ul>
            ) : (
                <p className="text-sm leading-6 text-muted">{emptyMessage}</p>
            )}
        </section>
    )
}

export function ReviewSummaryPanel({
    summary,
    isLoading,
    error,
    onRetry,
}: ReviewSummaryPanelProps) {
    if (isLoading && !summary) {
        return (
            <section
                className="mb-10 border border-[#d9dbca] bg-[#f6f7eb] px-5 py-7 min-[601px]:px-8"
                aria-live="polite"
                aria-busy="true"
                role="status"
            >
                <div className="flex items-center gap-3">
                    <Sparkles className="size-5 animate-pulse text-[#71801e]" aria-hidden="true" />
                    <div>
                        <h3 className="font-serif text-xl">AI 리뷰 요약을 확인하고 있습니다</h3>
                        <p className="mt-1 text-sm text-muted">리뷰 요약 상태를 잠시만 기다려 주세요.</p>
                    </div>
                </div>
            </section>
        )
    }

    if (!summary && error) {
        return (
            <section
                className="mb-10 border border-[#e2c7bf] bg-[#fff8f5] px-5 py-7 min-[601px]:px-8"
                aria-live="polite"
            >
                <div className="flex gap-3">
                    <CircleAlert className="mt-0.5 size-5 shrink-0 text-[#b34c35]" aria-hidden="true" />
                    <div>
                        <h3 className="font-serif text-xl">AI 리뷰 요약을 불러오지 못했습니다</h3>
                        <p className="mt-2 text-sm leading-6 text-[#68615e]">
                            원본 리뷰는 아래에서 계속 확인할 수 있습니다.
                        </p>
                        <button
                            className="mt-4 inline-flex items-center gap-2 border border-ink bg-white px-4 py-2 text-xs font-bold"
                            onClick={onRetry}
                            type="button"
                        >
                            <RefreshCw className="size-3.5" aria-hidden="true" />
                            다시 시도
                        </button>
                    </div>
                </div>
            </section>
        )
    }

    if (!summary?.available) {
        return (
            <section className="mb-10 border border-[#d9dbca] bg-[#f6f7eb] px-5 py-7 min-[601px]:px-8">
                <div className="flex gap-3">
                    <Sparkles className="mt-0.5 size-5 shrink-0 text-[#71801e]" aria-hidden="true" />
                    <div>
                        <p className="mb-1 text-[10px] font-extrabold tracking-[.16em] text-[#71801e]">
                            AI REVIEW SUMMARY
                        </p>
                        <h3 className="font-serif text-xl">리뷰가 더 쌓이면 핵심 의견을 요약해 드립니다</h3>
                        <p className="mt-2 text-sm leading-6 text-muted">
                            현재 등록된 리뷰는 {summary?.reviewCount ?? 0}개입니다. 원본 리뷰를 먼저
                            확인해 주세요.
                        </p>
                    </div>
                </div>
            </section>
        )
    }

    const generatedAt = formatGeneratedAt(summary.generatedAt)

    return (
        <section className="mb-10 border border-[#ccd1aa] bg-[#f6f7eb] px-5 py-7 min-[601px]:px-8 min-[601px]:py-8">
            <div className="flex flex-col gap-4 border-b border-[#d9dbca] pb-6 min-[701px]:flex-row min-[701px]:items-start min-[701px]:justify-between">
                <div>
                    <p className="mb-1 flex items-center gap-2 text-[10px] font-extrabold tracking-[.16em] text-[#71801e]">
                        <Sparkles className="size-3.5" aria-hidden="true" />
                        AI REVIEW SUMMARY
                    </p>
                    <h3 className="font-serif text-2xl">AI가 정리한 리뷰 핵심</h3>
                    <p className="mt-2 text-sm leading-6 text-muted">
                        총 {summary.reviewCount}개의 리뷰를 바탕으로 생성한 참고용 요약입니다.
                    </p>
                </div>
                {generatedAt && (
                    <p className="shrink-0 text-xs text-muted">
                        생성 {generatedAt}
                    </p>
                )}
            </div>

            {error && (
                <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border border-[#e2c7bf] bg-[#fff8f5] px-4 py-3 text-xs text-[#76564f]" role="status">
                    <span>최신 요약 확인에 실패해 기존 요약을 표시하고 있습니다.</span>
                    <button className="font-bold underline underline-offset-4" onClick={onRetry} type="button">
                        다시 시도
                    </button>
                </div>
            )}

            <div className="grid gap-5 py-7 min-[801px]:grid-cols-3 min-[801px]:gap-6">
                <SummaryColumn
                    icon={<ThumbsUp className="size-4 text-[#71801e]" aria-hidden="true" />}
                    title="좋았던 점"
                    items={summary.pros}
                    emptyMessage="공통으로 확인된 장점이 없습니다."
                />
                <SummaryColumn
                    icon={<ThumbsDown className="size-4 text-[#a05b4a]" aria-hidden="true" />}
                    title="아쉬웠던 점"
                    items={summary.cons}
                    emptyMessage="공통으로 확인된 단점이 없습니다."
                />
                <SummaryColumn
                    icon={<MessageCircleMore className="size-4 text-[#5f6650]" aria-hidden="true" />}
                    title="공통 의견"
                    items={summary.commonOpinions}
                    emptyMessage="반복해서 언급된 의견이 없습니다."
                />
            </div>

            <p className="border-t border-[#d9dbca] pt-4 text-xs leading-5 text-muted">
                AI 요약은 리뷰의 일부 맥락이나 소수 의견을 놓칠 수 있습니다. 구매 전 아래 원본
                리뷰를 함께 확인해 주세요.
            </p>
        </section>
    )
}
