import { X } from 'lucide-react'
import { useState } from 'react'
import { CATEGORY_LABELS } from '../../pages/supportPresentation'
import type {
    SupportInquiryCategory,
    SupportInquiryCreateRequest,
} from '../../types/support'

const CUSTOMER_CATEGORIES: SupportInquiryCategory[] = [
    'ORDER', 'PAYMENT', 'CANCEL_REFUND', 'DELIVERY', 'ACCOUNT', 'SERVICE',
]
const SELLER_CATEGORIES: SupportInquiryCategory[] = [
    'PRODUCT_APPROVAL', 'SETTLEMENT', 'SELLER_PERMISSION', 'POLICY', 'ACCOUNT', 'SERVICE',
]

export function InquiryCreateForm({
    seller,
    submitting,
    onCancel,
    onSubmit,
}: {
    seller: boolean
    submitting: boolean
    onCancel: () => void
    onSubmit: (request: SupportInquiryCreateRequest) => Promise<void>
}) {
    const categories = seller ? SELLER_CATEGORIES : CUSTOMER_CATEGORIES
    const [category, setCategory] = useState<SupportInquiryCategory>(categories[0])
    const [title, setTitle] = useState('')
    const [content, setContent] = useState('')

    return (
        <form
            className="mx-auto grid max-w-180 gap-5 p-6 min-[601px]:p-10"
            onSubmit={(event) => {
                event.preventDefault()
                void onSubmit({ category, title: title.trim(), content: content.trim() })
            }}
        >
            <div className="flex items-center justify-between gap-3">
                <div>
                    <p className="text-xs font-extrabold tracking-[.14em] text-accent">
                        NEW TICKET
                    </p>
                    <h2 className="mt-2 text-2xl font-extrabold">새 문의</h2>
                </div>
                <button
                    aria-label="문의 작성 취소"
                    className="grid size-10 place-items-center border border-line"
                    type="button"
                    onClick={onCancel}
                >
                    <X className="size-4" />
                </button>
            </div>
            <label className="grid gap-2 text-sm font-bold">
                문의 유형
                <select
                    className="h-12 border border-line bg-paper px-4 font-normal outline-none focus:border-ink"
                    value={category}
                    onChange={(event) => setCategory(event.target.value as SupportInquiryCategory)}
                >
                    {categories.map((value) => (
                        <option key={value} value={value}>{CATEGORY_LABELS[value]}</option>
                    ))}
                </select>
            </label>
            <label className="grid gap-2 text-sm font-bold">
                제목
                <input
                    className="h-12 border border-line bg-paper px-4 font-normal outline-none focus:border-ink"
                    required
                    maxLength={120}
                    value={title}
                    onChange={(event) => setTitle(event.target.value)}
                    placeholder="문의 제목을 입력해 주세요"
                />
            </label>
            <label className="grid gap-2 text-sm font-bold">
                문의 내용
                <textarea
                    className="min-h-60 resize-none border border-line bg-paper p-4 font-normal leading-7 outline-none focus:border-ink"
                    required
                    maxLength={2000}
                    value={content}
                    onChange={(event) => setContent(event.target.value)}
                    placeholder="문의 내용을 자세히 적어 주세요"
                />
                <span className="justify-self-end text-xs font-normal text-muted">
                    {content.length} / 2,000
                </span>
            </label>
            <div className="flex justify-end gap-2">
                <button
                    className="border border-line px-5 py-3 text-sm font-bold"
                    type="button"
                    onClick={onCancel}
                >
                    취소
                </button>
                <button
                    className="bg-ink px-6 py-3 text-sm font-bold text-paper disabled:opacity-50"
                    type="submit"
                    disabled={submitting || !title.trim() || !content.trim()}
                >
                    문의 등록
                </button>
            </div>
        </form>
    )
}
