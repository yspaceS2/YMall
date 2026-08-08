import type { FormEvent } from 'react'
import type { ProductQuestionRequest } from '../../types/productQuestion'

export function ProductQuestionForm({
    form,
    isEditing,
    isSaving,
    onChange,
    onCancel,
    onSubmit,
}: {
    form: ProductQuestionRequest
    isEditing: boolean
    isSaving: boolean
    onChange: (form: ProductQuestionRequest) => void
    onCancel: () => void
    onSubmit: (event: FormEvent<HTMLFormElement>) => void
}) {
    return (
        <form className="mb-8 grid gap-4 border border-line bg-paper p-5" onSubmit={onSubmit}>
            <label className="grid gap-2 text-xs font-bold">
                문의 제목
                <input
                    className="h-11 border border-line bg-surface px-3 text-sm font-normal outline-0 focus:border-ink"
                    maxLength={100}
                    value={form.title}
                    onChange={(event) => onChange({ ...form, title: event.target.value })}
                    required
                />
            </label>
            <label className="grid gap-2 text-xs font-bold">
                문의 내용
                <textarea
                    className="min-h-32 resize-y border border-line bg-surface p-3 text-sm font-normal leading-6 outline-0 focus:border-ink"
                    maxLength={2000}
                    value={form.content}
                    onChange={(event) => onChange({ ...form, content: event.target.value })}
                    required
                />
            </label>
            <label className="flex items-center gap-2 text-xs font-bold">
                <input
                    checked={form.privateQuestion}
                    type="checkbox"
                    onChange={(event) => onChange({
                        ...form,
                        privateQuestion: event.target.checked,
                    })}
                />
                비밀 문의로 등록
            </label>
            <div className="flex justify-end gap-2">
                <button className="h-10 border border-line px-4 text-xs font-bold" type="button" onClick={onCancel}>
                    취소
                </button>
                <button className="h-10 bg-ink px-5 text-xs font-bold text-white disabled:opacity-50" disabled={isSaving} type="submit">
                    {isSaving ? '저장 중...' : isEditing ? '문의 수정' : '문의 등록'}
                </button>
            </div>
        </form>
    )
}
