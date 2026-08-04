import { ChevronRight, LoaderCircle, Plus, Save, Tags, Trash2 } from 'lucide-react'
import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
    createAdminCategory,
    deleteAdminCategory,
    getAdminCategories,
    updateAdminCategory,
} from '../api/admin'
import { ApiError } from '../api/client'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import type { AdminCategory, AdminCategoryRequest } from '../types/admin'
import { useAdminAuthorization } from '../auth/useAdminAuthorization'

const emptyForm: AdminCategoryRequest = {
    name: '',
    slug: '',
    parentId: null,
    displayOrder: 0,
    active: true,
}

export function AdminCategoryManagementPage({ mode }: { mode: 'list' | 'new' | 'detail' }) {
    const { hasPermission } = useAdminAuthorization()
    const canCreate = hasPermission('CATEGORY_MANAGE_ALL')
    const canEdit = mode === 'new'
        ? canCreate
        : hasPermission('CATEGORY_MANAGE_PARTIAL', 'CATEGORY_MANAGE_ALL')
    const canDelete = hasPermission('CATEGORY_MANAGE_ALL')
    const { categoryId } = useParams()
    const navigate = useNavigate()
    const [searchParams, setSearchParams] = useSearchParams()
    const [categories, setCategories] = useState<AdminCategory[]>([])
    const [form, setForm] = useState<AdminCategoryRequest>(emptyForm)
    const [isLoading, setIsLoading] = useState(true)
    const [isSaving, setIsSaving] = useState(false)
    const [isDeleting, setIsDeleting] = useState(false)
    const [confirmDelete, setConfirmDelete] = useState(false)
    const [message, setMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')
    const selectedId = mode === 'detail' ? Number(categoryId) : null
    const selected = selectedId === null
        ? null
        : categories.find((category) => category.categoryId === selectedId) ?? null
    const keyword = searchParams.get('keyword') ?? ''
    const tree = useMemo(() => flattenTree(categories), [categories])
    const parentCandidates = categories.filter((category) =>
        category.depth < 3
        && category.categoryId !== selectedId
        && (selectedId === null || !isDescendant(categories, category.categoryId, selectedId)),
    )

    useEffect(() => {
        const controller = new AbortController()
        getAdminCategories('', controller.signal)
            .then((response) => {
                setCategories(response)
                if (mode === 'new') {
                    setForm(emptyForm)
                } else if (selectedId !== null) {
                    const selectedCategory = response.find(
                        (category) => category.categoryId === selectedId,
                    )
                    if (selectedCategory) {
                        setForm({
                            name: selectedCategory.name,
                            slug: selectedCategory.slug,
                            parentId: selectedCategory.parentId,
                            displayOrder: selectedCategory.displayOrder,
                            active: selectedCategory.active,
                        })
                    }
                }
                setErrorMessage('')
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(error instanceof ApiError
                    ? error.message
                    : '카테고리를 불러오지 못했습니다.')
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false)
            })
        return () => controller.abort()
    }, [mode, selectedId])

    async function save(event: FormEvent) {
        event.preventDefault()
        setIsSaving(true)
        setMessage('')
        setErrorMessage('')
        try {
            const saved = mode === 'new'
                ? await createAdminCategory(form)
                : await updateAdminCategory(selectedId as number, form)
            setCategories((current) => {
                const exists = current.some(
                    (category) => category.categoryId === saved.categoryId,
                )
                return exists
                    ? current.map((category) =>
                        category.categoryId === saved.categoryId ? saved : category)
                    : [...current, saved]
            })
            setMessage(mode === 'new'
                ? '카테고리를 등록했습니다.'
                : '카테고리를 수정했습니다.')
            navigate(`/admin/categories/${saved.categoryId}`, { replace: mode === 'new' })
        } catch (error) {
            setErrorMessage(error instanceof ApiError
                ? error.message
                : '카테고리를 저장하지 못했습니다.')
        } finally {
            setIsSaving(false)
        }
    }

    async function remove() {
        if (!selected) return
        setIsDeleting(true)
        setMessage('')
        setErrorMessage('')
        try {
            await deleteAdminCategory(selected.categoryId)
            setConfirmDelete(false)
            navigate('/admin/categories')
        } catch (error) {
            setErrorMessage(error instanceof ApiError
                ? error.message
                : '카테고리를 삭제하지 못했습니다.')
            setConfirmDelete(false)
        } finally {
            setIsDeleting(false)
        }
    }

    function submitSearch(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        const data = new FormData(event.currentTarget)
        const nextKeyword = String(data.get('keyword') ?? '').trim()
        const next = new URLSearchParams()
        if (nextKeyword) next.set('keyword', nextKeyword)
        setSearchParams(next)
    }

    const visibleTree = keyword
        ? tree.filter((category) =>
            category.name.toLowerCase().includes(keyword.toLowerCase())
            || category.slug.toLowerCase().includes(keyword.toLowerCase()))
        : tree

    return (
        <section className="mx-auto max-w-350 px-4 py-10 min-[601px]:px-8 min-[601px]:py-14">
            <header className="mb-8 flex flex-wrap items-end justify-between gap-4">
                <div>
                    <p className="text-[11px] font-extrabold tracking-[.18em] text-accent">
                        ADMIN CATEGORIES
                    </p>
                    <h1 className="mt-2 font-serif text-4xl font-semibold">카테고리 관리</h1>
                    <p className="mt-3 text-sm text-muted">
                        최대 3단계 카테고리를 구성하고 쇼핑몰 노출 순서를 관리합니다.
                    </p>
                </div>
                {canCreate && <Link
                    className="flex h-11 items-center gap-2 bg-ink px-5 text-xs font-bold text-white"
                    to="/admin/categories/new"
                >
                    <Plus className="size-4" />카테고리 등록
                </Link>}
            </header>

            {message && (
                <FeedbackMessage className="mb-5" tone="success">{message}</FeedbackMessage>
            )}
            {errorMessage && (
                <FeedbackMessage className="mb-5" tone="error">{errorMessage}</FeedbackMessage>
            )}

            <div className="grid gap-8 min-[1001px]:grid-cols-[minmax(280px,.7fr)_minmax(0,1.3fr)]">
                <aside className="min-w-0">
                    <form className="mb-4 flex" onSubmit={submitSearch} role="search">
                        <input
                            className="h-11 min-w-0 flex-1 border border-line bg-surface px-3 text-sm text-ink"
                            name="keyword"
                            defaultValue={keyword}
                            placeholder="이름 또는 슬러그 검색"
                        />
                        <button
                            className="h-11 bg-ink px-4 text-xs font-bold text-white"
                            type="submit"
                        >
                            검색
                        </button>
                    </form>
                    <div className="overflow-hidden border-y-2 border-ink">
                        {isLoading ? (
                            <div className="grid min-h-48 place-items-center">
                                <LoaderCircle className="size-5 animate-spin" />
                            </div>
                        ) : visibleTree.length === 0 ? (
                            <p className="p-5 text-sm text-muted">
                                조건에 맞는 카테고리가 없습니다.
                            </p>
                        ) : visibleTree.map((category) => (
                            <Link
                                className={[
                                    'flex items-center gap-2 border-b border-line py-3 pr-3 text-sm last:border-b-0 hover:bg-surface',
                                    selectedId === category.categoryId
                                        ? 'bg-surface font-bold'
                                        : '',
                                ].join(' ')}
                                style={{ paddingLeft: `${16 + (category.depth - 1) * 24}px` }}
                                key={category.categoryId}
                                to={`/admin/categories/${category.categoryId}${
                                    keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''
                                }`}
                                aria-current={selectedId === category.categoryId
                                    ? 'page'
                                    : undefined}
                            >
                                {category.depth > 1 && (
                                    <ChevronRight className="size-3.5 text-muted" />
                                )}
                                <span className="min-w-0 flex-1 truncate">{category.name}</span>
                                <span className="text-[10px] font-bold text-muted">
                                    D{category.depth}
                                </span>
                                {!category.active && (
                                    <span className="text-[10px] font-bold text-danger">
                                        숨김
                                    </span>
                                )}
                            </Link>
                        ))}
                    </div>
                </aside>

                <div className="min-w-0">
                    {mode === 'list' ? (
                        <div className="grid min-h-80 place-items-center border border-dashed border-line bg-surface p-8 text-center">
                            <div>
                                <Tags className="mx-auto size-8 text-muted" />
                                <h2 className="mt-4 font-bold">
                                    관리할 카테고리를 선택하세요
                                </h2>
                                <p className="mt-2 text-sm text-muted">
                                    왼쪽 트리에서 선택하거나 새 카테고리를 등록할 수 있습니다.
                                </p>
                            </div>
                        </div>
                    ) : selectedId !== null && !selected && !isLoading ? (
                        <FeedbackMessage tone="error">
                            카테고리를 찾을 수 없습니다.
                        </FeedbackMessage>
                    ) : (
                        <form className="grid overflow-hidden border-y-2 border-ink" onSubmit={save}>
                            <CategoryField label="카테고리명">
                                <input
                                    className={inputClassName}
                                    value={form.name}
                                    readOnly={!canEdit}
                                    onChange={(event) =>
                                        setForm({ ...form, name: event.target.value })}
                                    maxLength={100}
                                    required
                                />
                            </CategoryField>
                            <CategoryField label="슬러그">
                                <input
                                    className={inputClassName}
                                    value={form.slug}
                                    readOnly={!canEdit}
                                    onChange={(event) => setForm({
                                        ...form,
                                        slug: normalizeSlug(event.target.value),
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
                                    onChange={(event) => setForm({
                                        ...form,
                                        parentId: event.target.value
                                            ? Number(event.target.value)
                                            : null,
                                    })}
                                >
                                    <option value="">최상위 카테고리</option>
                                    {parentCandidates.map((category) => (
                                        <option
                                            key={category.categoryId}
                                            value={category.categoryId}
                                        >
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
                                    onChange={(event) => setForm({
                                        ...form,
                                        displayOrder: Number(event.target.value),
                                    })}
                                    required
                                />
                            </CategoryField>
                            <CategoryField label="노출 상태">
                                <span className="flex h-11 items-center gap-3">
                                    <input
                                        type="checkbox"
                                        checked={form.active}
                                        disabled={!canEdit}
                                        onChange={(event) => setForm({
                                            ...form,
                                            active: event.target.checked,
                                        })}
                                    />
                                    <span className="text-sm font-normal">
                                        {form.active ? '쇼핑몰에 노출' : '숨김'}
                                    </span>
                                </span>
                            </CategoryField>
                            {selected && (
                                <CategoryField label="연결 상태">
                                    <p className="text-sm font-normal text-muted">
                                        {selected.hasChildren
                                            ? '하위 카테고리 있음'
                                            : '하위 카테고리 없음'}
                                        {' · '}
                                        {selected.hasProducts
                                            ? '연결 상품 있음'
                                            : '연결 상품 없음'}
                                    </p>
                                </CategoryField>
                            )}
                            {canEdit && <div className="flex flex-wrap gap-2 px-4 py-4 min-[701px]:pl-[180px]">
                                <button
                                    className="flex h-11 items-center gap-2 bg-ink px-5 text-xs font-bold text-white disabled:opacity-50"
                                    type="submit"
                                    disabled={isSaving || (mode === 'detail' && !selected)}
                                >
                                    <Save className="size-4" />
                                    {isSaving
                                        ? '저장 중...'
                                        : mode === 'new'
                                            ? '카테고리 등록'
                                            : '변경 저장'}
                                </button>
                                {selected && canDelete && (
                                    <button
                                        className="flex h-11 items-center gap-2 border border-danger px-5 text-xs font-bold text-danger disabled:opacity-40"
                                        type="button"
                                        disabled={selected.hasChildren || selected.hasProducts}
                                        onClick={() => setConfirmDelete(true)}
                                    >
                                        <Trash2 className="size-4" />삭제
                                    </button>
                                )}
                            </div>}
                        </form>
                    )}
                </div>
            </div>

            <ConfirmDialog
                open={confirmDelete}
                title="카테고리를 삭제할까요?"
                description={selected ? `'${selected.name}' 카테고리를 삭제합니다.` : ''}
                confirmLabel="카테고리 삭제"
                isPending={isDeleting}
                onCancel={() => setConfirmDelete(false)}
                onConfirm={() => void remove()}
            />
        </section>
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

function flattenTree(categories: AdminCategory[]) {
    const byParent = new Map<number | null, AdminCategory[]>()
    categories.forEach((category) => {
        const siblings = byParent.get(category.parentId) ?? []
        siblings.push(category)
        byParent.set(category.parentId, siblings)
    })
    byParent.forEach((siblings) => siblings.sort(
        (left, right) =>
            left.displayOrder - right.displayOrder
            || left.name.localeCompare(right.name, 'ko'),
    ))
    const result: AdminCategory[] = []
    const visit = (parentId: number | null) => {
        ;(byParent.get(parentId) ?? []).forEach((category) => {
            result.push(category)
            visit(category.categoryId)
        })
    }
    visit(null)
    return result
}

function isDescendant(categories: AdminCategory[], candidateId: number, categoryId: number) {
    let cursor = categories.find((category) => category.categoryId === candidateId)
    while (cursor?.parentId) {
        if (cursor.parentId === categoryId) return true
        cursor = categories.find((category) => category.categoryId === cursor?.parentId)
    }
    return false
}

function normalizeSlug(value: string) {
    return value.toLowerCase().replace(/[^a-z0-9-]/g, '').replace(/-{2,}/g, '-')
}

const inputClassName =
    'h-11 w-full border border-line bg-surface px-3 text-sm font-normal text-ink'
