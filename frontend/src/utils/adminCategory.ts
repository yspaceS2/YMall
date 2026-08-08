import type { AdminCategory } from '../types/admin'

export function flattenAdminCategoryTree(categories: AdminCategory[]) {
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

export function isAdminCategoryDescendant(
    categories: AdminCategory[],
    candidateId: number,
    categoryId: number,
) {
    let cursor = categories.find((category) => category.categoryId === candidateId)
    while (cursor?.parentId) {
        if (cursor.parentId === categoryId) return true
        cursor = categories.find((category) => category.categoryId === cursor?.parentId)
    }
    return false
}

export function normalizeAdminCategorySlug(value: string) {
    return value.toLowerCase().replace(/[^a-z0-9-]/g, '').replace(/-{2,}/g, '-')
}
