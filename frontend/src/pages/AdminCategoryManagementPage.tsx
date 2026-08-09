import { Plus } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAdminAuthorization } from '../auth/useAdminAuthorization'
import { AdminCategoryForm } from '../components/admin/AdminCategoryForm'
import { AdminCategoryTree } from '../components/admin/AdminCategoryTree'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { FeedbackMessage } from '../components/ui/FeedbackMessage'
import {
    useAdminCategoryManagement,
    type AdminCategoryPageMode,
} from '../hooks/useAdminCategoryManagement'

export function AdminCategoryManagementPage({ mode }: { mode: AdminCategoryPageMode }) {
    const { hasPermission } = useAdminAuthorization()
    const canCreate = hasPermission('CATEGORY_MANAGE_ALL')
    const canEdit = mode === 'new'
        ? canCreate
        : hasPermission('CATEGORY_MANAGE_PARTIAL', 'CATEGORY_MANAGE_ALL')
    const canDelete = hasPermission('CATEGORY_MANAGE_ALL')
    const category = useAdminCategoryManagement(mode)

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
                {canCreate && (
                    <Link
                        className="flex h-11 items-center gap-2 bg-ink px-5 text-xs font-bold text-white"
                        to="/admin/categories/new"
                    >
                        <Plus className="size-4" />카테고리 등록
                    </Link>
                )}
            </header>

            {category.message && (
                <FeedbackMessage className="mb-5" tone="success">
                    {category.message}
                </FeedbackMessage>
            )}
            {category.errorMessage && (
                <FeedbackMessage className="mb-5" tone="error">
                    {category.errorMessage}
                </FeedbackMessage>
            )}

            <div className="grid gap-8 min-[1001px]:grid-cols-[minmax(280px,.7fr)_minmax(0,1.3fr)]">
                <AdminCategoryTree
                    categories={category.visibleTree}
                    selectedId={category.selectedId}
                    keyword={category.keyword}
                    isLoading={category.isLoading}
                    onSearch={category.submitSearch}
                />
                <div className="min-w-0">
                    <AdminCategoryForm
                        mode={mode}
                        form={category.form}
                        selected={category.selected}
                        selectedId={category.selectedId}
                        parentCandidates={category.parentCandidates}
                        isLoading={category.isLoading}
                        isSaving={category.isSaving}
                        canEdit={canEdit}
                        canDelete={canDelete}
                        setForm={category.setForm}
                        onDelete={() => category.setConfirmDelete(true)}
                        onSubmit={category.save}
                    />
                </div>
            </div>

            <ConfirmDialog
                open={category.confirmDelete}
                title="카테고리를 삭제할까요?"
                description={category.selected
                    ? `'${category.selected.name}' 카테고리를 삭제합니다.`
                    : ''}
                confirmLabel="카테고리 삭제"
                isPending={category.isDeleting}
                onCancel={() => category.setConfirmDelete(false)}
                onConfirm={() => void category.remove()}
            />
        </section>
    )
}
