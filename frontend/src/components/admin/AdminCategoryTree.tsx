import { ChevronRight, LoaderCircle } from 'lucide-react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import type { AdminCategory } from '../../types/admin'

interface AdminCategoryTreeProps {
    categories: AdminCategory[]
    selectedId: number | null
    keyword: string
    isLoading: boolean
    onSearch: (event: FormEvent<HTMLFormElement>) => void
}

export function AdminCategoryTree({
    categories,
    selectedId,
    keyword,
    isLoading,
    onSearch,
}: AdminCategoryTreeProps) {
    return (
        <aside className="min-w-0">
            <form className="mb-4 flex" onSubmit={onSearch} role="search">
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
                ) : categories.length === 0 ? (
                    <p className="p-5 text-sm text-muted">조건에 맞는 카테고리가 없습니다.</p>
                ) : categories.map((category) => (
                    <Link
                        aria-current={selectedId === category.categoryId ? 'page' : undefined}
                        className={[
                            'flex items-center gap-2 border-b border-line py-3 pr-3 text-sm last:border-b-0 hover:bg-surface',
                            selectedId === category.categoryId ? 'bg-surface font-bold' : '',
                        ].join(' ')}
                        key={category.categoryId}
                        style={{ paddingLeft: `${16 + (category.depth - 1) * 24}px` }}
                        to={`/admin/categories/${category.categoryId}${
                            keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''
                        }`}
                    >
                        {category.depth > 1 && <ChevronRight className="size-3.5 text-muted" />}
                        <span className="min-w-0 flex-1 truncate">{category.name}</span>
                        <span className="text-[10px] font-bold text-muted">D{category.depth}</span>
                        {!category.active && (
                            <span className="text-[10px] font-bold text-danger">숨김</span>
                        )}
                    </Link>
                ))}
            </div>
        </aside>
    )
}
