import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
    createAdminCategory,
    deleteAdminCategory,
    getAdminCategories,
    updateAdminCategory,
} from '../api/admin'
import { ApiError } from '../api/client'
import type { AdminCategory, AdminCategoryRequest } from '../types/admin'
import {
    flattenAdminCategoryTree,
    isAdminCategoryDescendant,
} from '../utils/adminCategory'

export type AdminCategoryPageMode = 'list' | 'new' | 'detail'

const emptyForm: AdminCategoryRequest = {
    name: '',
    slug: '',
    parentId: null,
    displayOrder: 0,
    active: true,
}

export function useAdminCategoryManagement(mode: AdminCategoryPageMode) {
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
    const tree = useMemo(() => flattenAdminCategoryTree(categories), [categories])
    const visibleTree = useMemo(() => {
        if (!keyword) return tree
        const normalizedKeyword = keyword.toLowerCase()
        return tree.filter((category) =>
            category.name.toLowerCase().includes(normalizedKeyword)
            || category.slug.toLowerCase().includes(normalizedKeyword))
    }, [keyword, tree])
    const parentCandidates = categories.filter((category) =>
        category.depth < 3
        && category.categoryId !== selectedId
        && (selectedId === null
            || !isAdminCategoryDescendant(categories, category.categoryId, selectedId)),
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
                    if (selectedCategory) setForm(toCategoryRequest(selectedCategory))
                }
                setErrorMessage('')
            })
            .catch((error: unknown) => {
                if (error instanceof Error && error.name === 'AbortError') return
                setErrorMessage(
                    error instanceof ApiError ? error.message : '카테고리를 불러오지 못했습니다.',
                )
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
            setErrorMessage(
                error instanceof ApiError ? error.message : '카테고리를 저장하지 못했습니다.',
            )
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
            setErrorMessage(
                error instanceof ApiError ? error.message : '카테고리를 삭제하지 못했습니다.',
            )
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

    return {
        mode,
        form,
        setForm,
        selectedId,
        selected,
        parentCandidates,
        visibleTree,
        keyword,
        isLoading,
        isSaving,
        isDeleting,
        confirmDelete,
        message,
        errorMessage,
        setConfirmDelete,
        submitSearch,
        save,
        remove,
    }
}

function toCategoryRequest(category: AdminCategory): AdminCategoryRequest {
    return {
        name: category.name,
        slug: category.slug,
        parentId: category.parentId,
        displayOrder: category.displayOrder,
        active: category.active,
    }
}
