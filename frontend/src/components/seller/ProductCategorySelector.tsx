import type { Category } from '../../types/product'
import {
    findCategoryPath,
    findFirstLeafCategoryId,
    getCategoryChildren,
} from '../../utils/productCategory'

interface ProductCategorySelectorProps {
    categories: Category[]
    value: number
    onChange: (categoryId: number) => void
}

export function ProductCategorySelector({
    categories,
    value,
    onChange,
}: ProductCategorySelectorProps) {
    const path = findCategoryPath(categories, value)
    const roots = getCategoryChildren(categories, null)
    const selectedRoot = path[0] ?? roots[0] ?? null
    const middleCategories = selectedRoot
        ? getCategoryChildren(categories, selectedRoot.categoryId)
        : []
    const selectedMiddle = path[1] ?? middleCategories[0] ?? null
    const leafCategories = selectedMiddle
        ? getCategoryChildren(categories, selectedMiddle.categoryId)
        : []
    const selectedLeaf = path[2] ?? leafCategories[0] ?? null

    return (
        <span className="grid gap-3">
            <span className="grid gap-2 min-[701px]:grid-cols-3">
                <CategorySelect
                    label="대분류"
                    value={selectedRoot?.categoryId ?? 0}
                    categories={roots}
                    onChange={(categoryId) => onChange(
                        findFirstLeafCategoryId(categories, categoryId),
                    )}
                />
                <CategorySelect
                    label="중분류"
                    value={selectedMiddle?.categoryId ?? 0}
                    categories={middleCategories}
                    onChange={(categoryId) => onChange(
                        findFirstLeafCategoryId(categories, categoryId),
                    )}
                    disabled={!selectedRoot || middleCategories.length === 0}
                />
                <CategorySelect
                    label="소분류"
                    value={selectedLeaf?.categoryId ?? 0}
                    categories={leafCategories}
                    onChange={onChange}
                    disabled={!selectedMiddle || leafCategories.length === 0}
                />
            </span>
            {path.length > 0 && (
                <span className="font-normal text-muted">
                    선택: {path.map((category) => category.name).join(' › ')}
                </span>
            )}
        </span>
    )
}

function CategorySelect({
    label,
    categories,
    value,
    onChange,
    disabled = false,
}: {
    label: string
    categories: Category[]
    value: number
    onChange: (categoryId: number) => void
    disabled?: boolean
}) {
    return (
        <label className="grid gap-1.5 font-normal">
            <span className="text-[11px] font-bold text-muted">{label}</span>
            <select
                className="h-11 min-w-0 border border-line bg-surface px-3 text-sm text-ink disabled:opacity-50"
                value={value}
                disabled={disabled}
                onChange={(event) => onChange(Number(event.target.value))}
            >
                {categories.length === 0 && <option value={0}>선택 항목 없음</option>}
                {categories.map((category) => (
                    <option key={category.categoryId} value={category.categoryId}>
                        {category.name}
                    </option>
                ))}
            </select>
        </label>
    )
}
