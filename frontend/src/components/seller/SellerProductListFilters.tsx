import type { Category } from '../../types/product'
import type { SellerProductStockCondition } from '../../types/seller'

interface SellerProductListFiltersProps {
    rootCategoryId?: number
    middleCategoryId?: number
    leafCategoryId?: number
    rootCategories: Category[]
    middleCategories: Category[]
    leafCategories: Category[]
    stockCondition: SellerProductStockCondition
    stockQuantity?: number
    onFilterChange: (name: string, value: string, childNames?: string[]) => void
}

export function SellerProductListFilters({
    rootCategoryId,
    middleCategoryId,
    leafCategoryId,
    rootCategories,
    middleCategories,
    leafCategories,
    stockCondition,
    stockQuantity,
    onFilterChange,
}: SellerProductListFiltersProps) {
    return (
        <div className="mb-5 grid gap-3 md:grid-cols-2 xl:grid-cols-[1fr_1fr_1fr_1.2fr]">
            <CategoryFilter
                label="대분류"
                emptyLabel="전체 대분류"
                categories={rootCategories}
                value={rootCategoryId}
                onChange={(value) => onFilterChange(
                    'rootCategoryId',
                    value,
                    ['middleCategoryId', 'categoryId'],
                )}
            />
            <CategoryFilter
                label="중분류"
                emptyLabel="전체 중분류"
                categories={middleCategories}
                disabled={!rootCategoryId}
                value={middleCategoryId}
                onChange={(value) => onFilterChange('middleCategoryId', value, ['categoryId'])}
            />
            <CategoryFilter
                label="소분류"
                emptyLabel="전체 소분류"
                categories={leafCategories}
                disabled={!middleCategoryId}
                value={leafCategoryId}
                onChange={(value) => onFilterChange('categoryId', value)}
            />
            <label className="grid gap-1.5 text-xs font-bold">
                재고 수량
                <span className="flex">
                    <input
                        className="h-11 min-w-0 flex-1 border border-r-0 border-line bg-surface px-3 text-sm font-normal text-ink outline-0 focus:border-ink"
                        min="0"
                        placeholder="수량 입력"
                        type="number"
                        value={stockQuantity ?? ''}
                        onChange={(event) => onFilterChange('stockQuantity', event.target.value)}
                    />
                    <select
                        aria-label="재고 수량 비교 조건"
                        className="h-11 border border-line bg-surface px-3 text-sm font-normal text-ink outline-0 focus:border-ink"
                        value={stockCondition}
                        onChange={(event) => onFilterChange('stockCondition', event.target.value)}
                    >
                        <option value="GTE">개 이상</option>
                        <option value="LTE">개 이하</option>
                    </select>
                </span>
            </label>
        </div>
    )
}

function CategoryFilter({
    label,
    emptyLabel,
    categories,
    disabled = false,
    value,
    onChange,
}: {
    label: string
    emptyLabel: string
    categories: Category[]
    disabled?: boolean
    value?: number
    onChange: (value: string) => void
}) {
    return (
        <label className="grid gap-1.5 text-xs font-bold">
            {label}
            <select
                className="h-11 border border-line bg-surface px-3 text-sm font-normal text-ink outline-0 focus:border-ink"
                disabled={disabled}
                value={value ?? ''}
                onChange={(event) => onChange(event.target.value)}
            >
                <option value="">{emptyLabel}</option>
                {categories.map((category) => (
                    <option key={category.categoryId} value={category.categoryId}>
                        {category.name}
                    </option>
                ))}
            </select>
        </label>
    )
}
