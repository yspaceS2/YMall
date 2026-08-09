import { Save, Tags, Trash2 } from 'lucide-react'
import type { Dispatch, FormEvent, ReactNode, SetStateAction } from 'react'
import type { AdminCategory, AdminCategoryRequest } from '../../types/admin'
import type { AdminCategoryPageMode } from '../../hooks/useAdminCategoryManagement'
import { normalizeAdminCategorySlug } from '../../utils/adminCategory'
import { FeedbackMessage } from '../ui/FeedbackMessage'

interface AdminCategoryFormProps {
    mode: AdminCategoryPageMode
    form: AdminCategoryRequest
    selected: AdminCategory | null
    selectedId: number | null
    parentCandidates: AdminCategory[]
    isLoading: boolean
    isSaving: boolean
    canEdit: boolean
    canDelete: boolean
    setForm: Dispatch<SetStateAction<AdminCategoryRequest>>
    onDelete: () => void
    onSubmit: (event: FormEvent) => void
}

export function AdminCategoryForm({
    mode,
    form,
    selected,
    selectedId,
    parentCandidates,
    isLoading,
    isSaving,
    canEdit,
    canDelete,
    setForm,
    onDelete,
    onSubmit,
}: AdminCategoryFormProps) {
    function updateForm(values: Partial<AdminCategoryRequest>) {
        setForm((current) => ({ ...current, ...values }))
    }

    if (mode === 'list') {
        return (
            <div className="grid min-h-80 place-items-center border border-dashed border-line bg-surface p-8 text-center">
                <div>
                    <Tags className="mx-auto size-8 text-muted" />
                    <h2 className="mt-4 font-bold">관리할 카테고리를 선택하세요</h2>
                    <p className="mt-2 text-sm text-muted">
                        왼쪽 트리에서 선택하거나 새 카테고리를 등록할 수 있습니다.
                    </p>
                </div>
            </div>
        )
    }

    if (selectedId !== null && !selected && !isLoading) {
        return <FeedbackMessage tone="error">카테고리를 찾을 수 없습니다.</FeedbackMessage>
    }

    return (
        <form className="grid overflow-hidden border-y-2 border-ink" onSubmit={onSubmit}>
            <CategoryField label="카테고리명">
                <input
                    className={inputClassName}
                    value={form.name}
                    readOnly={!canEdit}
                    onChange={(event) => updateForm({ name: event.target.value })}
                    maxLength={100}
                    required
                />
            </CategoryField>
            <CategoryField label="슬러그">
                <input
                    className={inputClassName}
                    value={form.slug}
                    readOnly={!canEdit}
                    onChange={(event) => updateForm({
                        slug: normalizeAdminCategorySlug(event.target.value),
                    })}
                    maxLength={100}
                    pattern="[a-z0-9]+(?:-[a-z0-9]+)*"
                    placeholder="fashion-women"
                    required
                />
            </CategoryField>
            <CategoryField label="상위 카테고리">
                <select
                    className={inputClassName}
                    value={form.parentId ?? ''}
                    disabled={!canEdit}
                    onChange={(event) => updateForm({
                        parentId: event.target.value ? Number(event.target.value) : null,
                    })}
                >
                    <option value="">최상위 카테고리</option>
                    {parentCandidates.map((category) => (
                        <option key={category.categoryId} value={category.categoryId}>
                            {'　'.repeat(category.depth - 1)}{category.name}
                        </option>
                    ))}
                </select>
            </CategoryField>
            <CategoryField label="노출 순서">
                <input
                    className={inputClassName}
                    type="number"
                    min={0}
                    max={9999}
                    value={form.displayOrder}
                    readOnly={!canEdit}
                    onChange={(event) => updateForm({ displayOrder: Number(event.target.value) })}
                    required
                />
            </CategoryField>
            <CategoryField label="노출 상태">
                <span className="flex h-11 items-center gap-3">
                    <input
                        type="checkbox"
                        checked={form.active}
                        disabled={!canEdit}
                        onChange={(event) => updateForm({ active: event.target.checked })}
                    />
                    <span className="text-sm font-normal">
                        {form.active ? '쇼핑몰에 노출' : '숨김'}
                    </span>
                </span>
            </CategoryField>
            {selected && (
                <CategoryField label="연결 상태">
                    <p className="text-sm font-normal text-muted">
                        {selected.hasChildren ? '하위 카테고리 있음' : '하위 카테고리 없음'}
                        {' · '}
                        {selected.hasProducts ? '연결 상품 있음' : '연결 상품 없음'}
                    </p>
                </CategoryField>
            )}
            {canEdit && (
                <div className="flex flex-wrap gap-2 px-4 py-4 min-[701px]:pl-[180px]">
                    <button
                        className="flex h-11 items-center gap-2 bg-ink px-5 text-xs font-bold text-white disabled:opacity-50"
                        type="submit"
                        disabled={isSaving || (mode === 'detail' && !selected)}
                    >
                        <Save className="size-4" />
                        {isSaving
                            ? '저장 중...'
                            : mode === 'new' ? '카테고리 등록' : '변경 저장'}
                    </button>
                    {selected && canDelete && (
                        <button
                            className="flex h-11 items-center gap-2 border border-danger px-5 text-xs font-bold text-danger disabled:opacity-40"
                            type="button"
                            disabled={selected.hasChildren || selected.hasProducts}
                            onClick={onDelete}
                        >
                            <Trash2 className="size-4" />삭제
                        </button>
                    )}
                </div>
            )}
        </form>
    )
}

function CategoryField({ label, children }: { label: string; children: ReactNode }) {
    return (
        <label className="grid gap-2 border-b border-line px-4 py-4 text-xs font-bold last:border-b-0 min-[701px]:grid-cols-[140px_minmax(0,1fr)] min-[701px]:items-center min-[701px]:gap-6">
            <span>{label}</span>
            {children}
        </label>
    )
}

const inputClassName =
    'h-11 w-full border border-line bg-surface px-3 text-sm font-normal text-ink'
