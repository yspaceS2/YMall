import type { Category } from '../types/product'

export function findFirstLeafCategoryId(categories: Category[], categoryId?: number) {
    const start = categoryId
        ? categories.find((category) => category.categoryId === categoryId)
        : getCategoryChildren(categories, null)[0]
    if (!start) return 0

    let current = start
    let children = getCategoryChildren(categories, current.categoryId)
    while (children.length > 0) {
        current = children[0]
        children = getCategoryChildren(categories, current.categoryId)
    }
    return current.categoryId
}

export function findCategoryPath(categories: Category[], categoryId: number) {
    const path: Category[] = []
    let current = categories.find((category) => category.categoryId === categoryId)
    while (current) {
        path.unshift(current)
        current = current.parentId == null
            ? undefined
            : categories.find((category) => category.categoryId === current?.parentId)
    }
    return path
}

export function getCategoryChildren(categories: Category[], parentId: number | null) {
    return categories
        .filter((category) => (category.parentId ?? null) === parentId)
        .sort((left, right) => (
            (left.displayOrder ?? 0) - (right.displayOrder ?? 0)
            || left.name.localeCompare(right.name, 'ko')
        ))
}
