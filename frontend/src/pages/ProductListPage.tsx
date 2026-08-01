import { Search, SlidersHorizontal } from 'lucide-react'
import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getCategories, getProducts } from '../api/products'
import { HomeEventCarousel } from '../components/HomeEventCarousel'
import { HomeMerchandisingSections } from '../components/HomeMerchandisingSections'
import { ProductCard } from '../components/ProductCard'
import { PageState } from '../components/ui/PageState'
import { useProductSearchSuggestions } from '../hooks/useProductSearchSuggestions'
import type { Category, PageResponse, ProductSummary } from '../types/product'
import { findCategoryPath, getCategoryChildren } from '../utils/productCategory'

const PAGE_SIZE = 12

export function ProductListPage() {
    const [searchParams, setSearchParams] = useSearchParams()
    const query = searchParams.get('keyword')?.trim() ?? ''
    const categoryIdParameter = Number(searchParams.get('categoryId'))
    const categoryId = Number.isSafeInteger(categoryIdParameter) && categoryIdParameter > 0
        ? categoryIdParameter
        : undefined
    const pageParameter = Number(searchParams.get('page'))
    const page = Number.isSafeInteger(pageParameter) && pageParameter > 0 ? pageParameter : 1
    const showCatalog = Boolean(query || categoryId || searchParams.get('view') === 'all')
    const showHomeMerchandising = !showCatalog
    const [categories, setCategories] = useState<Category[]>([])
    const rootCategories = useMemo(
        () => categories.filter((category) => (category.parentId ?? null) === null),
        [categories],
    )
    const categoryPath = useMemo(
        () => categoryId ? findCategoryPath(categories, categoryId) : [],
        [categories, categoryId],
    )
    const selectedCategory = categoryPath.at(-1) ?? null
    const selectedCategoryChildren = useMemo(
        () => selectedCategory
            ? getCategoryChildren(categories, selectedCategory.categoryId)
            : [],
        [categories, selectedCategory],
    )
    const tabContextCategory = selectedCategory
        ? selectedCategoryChildren.length > 0
            ? selectedCategory
            : categoryPath.at(-2) ?? selectedCategory
        : null
    const categoryTabs = useMemo(
        () => tabContextCategory
            ? getCategoryChildren(categories, tabContextCategory.categoryId)
            : rootCategories,
        [categories, rootCategories, tabContextCategory],
    )
    const catalogTitle = selectedCategory && query
        ? `${selectedCategory.name} 내 '${query}' 검색 결과`
        : selectedCategory?.name ?? (query ? `'${query}' 검색 결과` : '전체 상품')
    const [retryKey, setRetryKey] = useState(0)
    const requestKey = `${page}:${categoryId ?? 'all'}:${query}:${retryKey}`
    const [result, setResult] = useState<{
        key: string
        products: PageResponse<ProductSummary> | null
        error: string
    }>({ key: '', products: null, error: '' })
    const loading = result.key !== requestKey
    const { products, error } = result

    useEffect(() => {
        if (!showCatalog) {
            return
        }

        const controller = new AbortController()
        getCategories(controller.signal).then(setCategories).catch(() => setCategories([]))
        return () => controller.abort()
    }, [showCatalog])

    useEffect(() => {
        if (!showCatalog) {
            return
        }

        const controller = new AbortController()

        getProducts({ page, size: PAGE_SIZE, keyword: query || undefined, categoryId, signal: controller.signal })
            .then((data) => setResult({ key: requestKey, products: data, error: '' }))
            .catch((requestError: unknown) => {
                if (requestError instanceof Error && requestError.name !== 'AbortError') {
                    setResult({ key: requestKey, products: null, error: requestError.message })
                }
            })

        return () => controller.abort()
    }, [categoryId, page, query, requestKey, showCatalog])

    function selectCategory(nextCategoryId?: number) {
        const nextSearchParams = new URLSearchParams(searchParams)
        nextSearchParams.delete('keyword')
        nextSearchParams.delete('page')
        if (nextCategoryId) {
            nextSearchParams.delete('view')
            nextSearchParams.set('categoryId', String(nextCategoryId))
        } else {
            nextSearchParams.delete('categoryId')
            nextSearchParams.set('view', 'all')
        }
        setSearchParams(nextSearchParams)
    }

    function submitSearch(nextKeywordValue: string) {
        const nextSearchParams = new URLSearchParams(searchParams)
        const nextKeyword = nextKeywordValue.trim()
        nextSearchParams.delete('page')
        nextSearchParams.delete('view')
        if (nextKeyword) {
            nextSearchParams.set('keyword', nextKeyword)
        } else {
            nextSearchParams.delete('keyword')
        }
        setSearchParams(nextSearchParams)
    }

    function selectPage(nextPage: number) {
        const nextSearchParams = new URLSearchParams(searchParams)
        if (nextPage > 1) {
            nextSearchParams.set('page', String(nextPage))
        } else {
            nextSearchParams.delete('page')
        }
        setSearchParams(nextSearchParams)
    }

    return (
        <>
            {showHomeMerchandising && <HomeEventCarousel />}
            {showHomeMerchandising && <HomeMerchandisingSections />}

            {showCatalog && <section className="mx-auto max-w-360 border-t border-line px-4 pt-12 pb-20 min-[601px]:px-[clamp(20px,5vw,72px)] min-[601px]:pt-18 min-[601px]:pb-27.5">
                <div className="flex flex-col items-start justify-between gap-6 min-[601px]:flex-row min-[601px]:items-end">
                    <div>
                        {categoryPath.length > 0 && (
                            <nav
                                className="mb-3 flex flex-wrap items-center gap-2 text-xs text-muted"
                                aria-label="카테고리 경로"
                            >
                                {categoryPath.map((category, index) => (
                                    <span
                                        className="inline-flex items-center gap-2"
                                        key={category.categoryId}
                                    >
                                        {index > 0 && <span aria-hidden="true">›</span>}
                                        <button
                                            className={`border-0 bg-transparent p-0 ${
                                                index === categoryPath.length - 1
                                                    ? 'font-bold text-ink'
                                                    : 'hover:text-ink'
                                            }`}
                                            type="button"
                                            onClick={() => selectCategory(category.categoryId)}
                                        >
                                            {category.name}
                                        </button>
                                    </span>
                                ))}
                            </nav>
                        )}
                        <span className="text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">
                            SHOP
                        </span>
                        <h2 className="mt-2 mr-3 inline font-serif text-[34px] leading-tight font-semibold">
                            {catalogTitle}
                        </h2>
                        <p className="inline text-[13px] text-muted">
                            {products?.totalElements ?? 0}개의 상품
                        </p>
                    </div>
                    <CatalogProductSearch
                        categoryId={categoryId}
                        categoryName={selectedCategory?.name}
                        initialKeyword={query}
                        key={query}
                        onSearch={submitSearch}
                    />
                </div>

                <div className="mt-9.5 mb-8 flex items-center justify-between border-b border-line">
                    <div className="flex gap-7 overflow-x-auto">
                        <button
                            className={`border-0 border-b-2 bg-transparent py-3.5 text-[13px] whitespace-nowrap ${
                                tabContextCategory
                                    ? categoryId === tabContextCategory.categoryId
                                        ? 'border-ink font-extrabold text-ink'
                                        : 'border-transparent text-muted'
                                    : !categoryId && !query
                                        ? 'border-ink font-extrabold text-ink'
                                        : 'border-transparent text-muted'
                            }`}
                            onClick={() => selectCategory(tabContextCategory?.categoryId)}
                            type="button"
                        >
                            {tabContextCategory ? `${tabContextCategory.name} 전체` : 'ALL'}
                        </button>
                        {categoryTabs.map((category) => (
                            <button className={`border-0 border-b-2 bg-transparent py-3.5 text-[13px] whitespace-nowrap ${categoryId === category.categoryId ? 'border-ink font-extrabold text-ink' : 'border-transparent text-muted'}`} onClick={() => selectCategory(category.categoryId)} key={category.categoryId} type="button">{category.name}</button>
                        ))}
                    </div>
                    <span className="hidden items-center gap-2 text-xs min-[601px]:flex"><SlidersHorizontal className="size-4" /> 추천순</span>
                </div>

                {loading && <PageState variant="loading" title="상품을 불러오는 중입니다" description="잠시만 기다려 주세요." />}
                {!loading && error && (
                    <PageState
                        variant="error"
                        title="상품을 불러오지 못했습니다"
                        description={error}
                        action={<button className="border border-ink bg-white px-5 py-2.5 text-xs font-bold" type="button" onClick={() => setRetryKey((value) => value + 1)}>다시 시도</button>}
                    />
                )}
                {!loading && !error && products?.content.length === 0 && <PageState variant="empty" title="조건에 맞는 상품이 없습니다" description="검색어나 카테고리를 변경해 보세요." />}
                {!loading && !error && products && products.content.length > 0 && (
                    <div className="grid grid-cols-2 gap-x-2.5 gap-y-8.5 min-[601px]:gap-x-5 min-[601px]:gap-y-11 min-[901px]:grid-cols-4">{products.content.map((product) => <ProductCard product={product} key={product.productId} />)}</div>
                )}

                {products && products.totalPages > 1 && (
                    <nav className="mt-17 flex justify-center gap-1" aria-label="상품 페이지">
                        <button className="h-9.5 min-w-9.5 border border-line bg-transparent disabled:opacity-35" disabled={!products.hasPrevious} onClick={() => selectPage(page - 1)} type="button">이전</button>
                        {Array.from({ length: products.totalPages }, (_, index) => index + 1).slice(Math.max(0, page - 3), page + 2).map((number) => (
                            <button className={`h-9.5 min-w-9.5 border border-line ${page === number ? 'bg-ink text-white' : 'bg-transparent'}`} onClick={() => selectPage(number)} key={number} type="button">{number}</button>
                        ))}
                        <button className="h-9.5 min-w-9.5 border border-line bg-transparent disabled:opacity-35" disabled={!products.hasNext} onClick={() => selectPage(page + 1)} type="button">다음</button>
                    </nav>
                )}
            </section>}
        </>
    )
}

function CatalogProductSearch({
    categoryId,
    categoryName,
    initialKeyword,
    onSearch,
}: {
    categoryId?: number
    categoryName?: string
    initialKeyword: string
    onSearch: (keyword: string) => void
}) {
    const containerRef = useRef<HTMLFormElement>(null)
    const [keyword, setKeyword] = useState(initialKeyword)
    const [isSuggestionsOpen, setIsSuggestionsOpen] = useState(true)
    const [activeSuggestionIndex, setActiveSuggestionIndex] = useState(-1)
    const {
        suggestions,
        isLoading,
        hasError,
        shouldShowSuggestions,
    } = useProductSearchSuggestions(keyword, isSuggestionsOpen, categoryId)

    useEffect(() => {
        const closeOnOutsidePointer = (event: PointerEvent) => {
            const target = event.target
            if (!(target instanceof Node)) return
            if (!containerRef.current?.contains(target)) setIsSuggestionsOpen(false)
        }
        document.addEventListener('pointerdown', closeOnOutsidePointer)
        return () => document.removeEventListener('pointerdown', closeOnOutsidePointer)
    }, [])

    const search = (nextKeyword: string) => {
        setKeyword(nextKeyword)
        setIsSuggestionsOpen(false)
        setActiveSuggestionIndex(-1)
        onSearch(nextKeyword)
    }

    const submit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        search(keyword)
    }

    return (
        <form
            ref={containerRef}
            className="relative flex w-full border-b border-ink min-[601px]:max-w-97.5"
            role="search"
            aria-label="상품 목록 검색"
            onSubmit={submit}
        >
            <input
                className="w-full border-0 bg-transparent px-1 py-3 text-[13px] outline-0"
                value={keyword}
                onChange={(event) => {
                    setKeyword(event.target.value)
                    setIsSuggestionsOpen(true)
                    setActiveSuggestionIndex(-1)
                }}
                onFocus={() => setIsSuggestionsOpen(true)}
                onKeyDown={(event) => {
                    if (event.key === 'Escape') {
                        setIsSuggestionsOpen(false)
                        return
                    }
                    if (suggestions.length === 0) return
                    if (event.key === 'ArrowDown') {
                        event.preventDefault()
                        setActiveSuggestionIndex((index) => (index + 1) % suggestions.length)
                    } else if (event.key === 'ArrowUp') {
                        event.preventDefault()
                        setActiveSuggestionIndex((index) => index <= 0 ? suggestions.length - 1 : index - 1)
                    } else if (event.key === 'Enter' && activeSuggestionIndex >= 0) {
                        event.preventDefault()
                        search(suggestions[activeSuggestionIndex].name)
                    }
                }}
                placeholder={categoryName
                    ? `${categoryName} 카테고리에서 검색해보세요`
                    : '찾고 있는 상품을 검색해보세요'}
                aria-label="상품 검색"
                role="combobox"
                aria-autocomplete="list"
                aria-controls="catalog-product-search-suggestions"
                aria-expanded={shouldShowSuggestions}
                aria-activedescendant={activeSuggestionIndex >= 0
                    ? `catalog-product-search-suggestion-${suggestions[activeSuggestionIndex].productId}`
                    : undefined}
            />
            <button className="border-0 bg-transparent" type="submit" aria-label="검색">
                <Search className="size-5" />
            </button>

            {shouldShowSuggestions && (
                <div
                    className="absolute top-full right-0 left-0 z-20 mt-2 overflow-hidden rounded-xl border border-line bg-paper shadow-[0_16px_40px_rgba(20,20,16,.16)]"
                    id="catalog-product-search-suggestions"
                    role="listbox"
                    aria-label="상품 목록 추천 검색어"
                >
                    {isLoading && <p className="px-4 py-3 text-sm text-muted" role="status">추천 검색어를 찾는 중입니다.</p>}
                    {!isLoading && hasError && <p className="px-4 py-3 text-sm text-muted" role="status">추천 검색어를 불러오지 못했습니다.</p>}
                    {!isLoading && !hasError && suggestions.length === 0 && (
                        <p className="px-4 py-3 text-sm text-muted" role="status">일치하는 추천 검색어가 없습니다.</p>
                    )}
                    {!isLoading && suggestions.map((suggestion, index) => (
                        <button
                            key={suggestion.productId}
                            id={`catalog-product-search-suggestion-${suggestion.productId}`}
                            className={`flex w-full items-center gap-3 px-4 py-3 text-left text-sm transition-colors ${
                                index === activeSuggestionIndex ? 'bg-surface' : 'hover:bg-surface'
                            }`}
                            type="button"
                            role="option"
                            aria-selected={index === activeSuggestionIndex}
                            onMouseEnter={() => setActiveSuggestionIndex(index)}
                            onClick={() => search(suggestion.name)}
                        >
                            <span className="min-w-0 flex-1 truncate font-semibold">{suggestion.name}</span>
                            {suggestion.matchType === 'FUZZY' && (
                                <span className="shrink-0 text-[10px] font-bold text-muted">유사 검색</span>
                            )}
                        </button>
                    ))}
                </div>
            )}
        </form>
    )
}
