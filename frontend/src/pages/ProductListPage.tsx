import { Search, SlidersHorizontal } from 'lucide-react'
import { useEffect, useState } from 'react'
import { getCategories, getProducts } from '../api/products'
import { ProductCard } from '../components/ProductCard'
import type { Category, PageResponse, ProductSummary } from '../types/product'

const PAGE_SIZE = 12

export function ProductListPage() {
    const [categories, setCategories] = useState<Category[]>([])
    const [categoryId, setCategoryId] = useState<number>()
    const [keyword, setKeyword] = useState('')
    const [query, setQuery] = useState('')
    const [page, setPage] = useState(1)
    const requestKey = `${page}:${categoryId ?? 'all'}:${query}`
    const [result, setResult] = useState<{
        key: string
        products: PageResponse<ProductSummary> | null
        error: string
    }>({ key: '', products: null, error: '' })
    const loading = result.key !== requestKey
    const { products, error } = result

    useEffect(() => {
        const controller = new AbortController()
        getCategories(controller.signal).then(setCategories).catch(() => setCategories([]))
        return () => controller.abort()
    }, [])

    useEffect(() => {
        const controller = new AbortController()

        getProducts({ page, size: PAGE_SIZE, keyword: query || undefined, categoryId, signal: controller.signal })
            .then((data) => setResult({ key: requestKey, products: data, error: '' }))
            .catch((requestError: unknown) => {
                if (requestError instanceof Error && requestError.name !== 'AbortError') {
                    setResult({ key: requestKey, products: null, error: requestError.message })
                }
            })

        return () => controller.abort()
    }, [categoryId, page, query, requestKey])

    function selectCategory(nextCategoryId?: number) {
        setCategoryId(nextCategoryId)
        setQuery('')
        setKeyword('')
        setPage(1)
    }

    return (
        <>
            <section className="flex min-h-72 flex-col justify-end border-b border-line bg-[radial-gradient(circle_at_82%_20%,rgba(217,255,67,.95),transparent_22%),linear-gradient(135deg,#e8e9df_0%,#f6f5ef_48%,#d6dbbb_100%)] px-5 py-15 sm:min-h-95 sm:px-[clamp(24px,7vw,110px)] sm:py-23">
                <p className="text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">CURATED FOR YOUR EVERYDAY</p>
                <h1 className="my-3 max-w-180 font-serif text-[clamp(42px,6vw,78px)] leading-[.95] font-medium tracking-[-.055em]">Discover your next favorite.</h1>
                <span className="text-[#64645e]">오래 곁에 둘 가치 있는 물건들을 소개합니다.</span>
            </section>

            <section className="mx-auto max-w-360 px-5 pt-18 pb-27.5 sm:px-[clamp(20px,5vw,72px)]">
                <div className="flex flex-col items-start justify-between gap-6 sm:flex-row sm:items-end">
                    <div><span className="text-[11px] font-extrabold tracking-[.18em] text-[#71801e]">SHOP</span><h2 className="mt-2 mr-3 inline font-serif text-[34px] leading-tight font-semibold">전체 상품</h2><p className="inline text-[13px] text-muted">{products?.totalElements ?? 0}개의 상품</p></div>
                    <form className="flex w-full border-b border-ink sm:max-w-97.5" onSubmit={(event) => { event.preventDefault(); setQuery(keyword.trim()); setCategoryId(undefined); setPage(1) }}>
                        <input className="w-full border-0 bg-transparent px-1 py-3 text-[13px] outline-0" value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="찾고 있는 상품을 검색해보세요" aria-label="상품 검색" />
                        <button className="border-0 bg-transparent" type="submit" aria-label="검색"><Search className="size-5" /></button>
                    </form>
                </div>

                <div className="mt-9.5 mb-8 flex items-center justify-between border-b border-line">
                    <div className="flex gap-7 overflow-x-auto">
                        <button className={`border-0 border-b-2 bg-transparent py-3.5 text-[13px] whitespace-nowrap ${!categoryId && !query ? 'border-ink font-extrabold text-ink' : 'border-transparent text-muted'}`} onClick={() => selectCategory()} type="button">ALL</button>
                        {categories.map((category) => (
                            <button className={`border-0 border-b-2 bg-transparent py-3.5 text-[13px] whitespace-nowrap ${categoryId === category.categoryId ? 'border-ink font-extrabold text-ink' : 'border-transparent text-muted'}`} onClick={() => selectCategory(category.categoryId)} key={category.categoryId} type="button">{category.name}</button>
                        ))}
                    </div>
                    <span className="hidden items-center gap-2 text-xs sm:flex"><SlidersHorizontal className="size-4" /> 추천순</span>
                </div>

                {loading && <StatusPanel title="상품을 불러오는 중입니다" />}
                {!loading && error && <StatusPanel title="상품을 불러오지 못했습니다" description={error} />}
                {!loading && !error && products?.content.length === 0 && <StatusPanel title="조건에 맞는 상품이 없습니다" description="검색어나 카테고리를 변경해 보세요." />}
                {!loading && !error && products && products.content.length > 0 && (
                    <div className="grid grid-cols-2 gap-x-2.5 gap-y-8.5 sm:gap-x-5 sm:gap-y-11 lg:grid-cols-4">{products.content.map((product) => <ProductCard product={product} key={product.productId} />)}</div>
                )}

                {products && products.totalPages > 1 && (
                    <nav className="mt-17 flex justify-center gap-1" aria-label="상품 페이지">
                        <button className="h-9.5 min-w-9.5 border border-line bg-transparent disabled:opacity-35" disabled={!products.hasPrevious} onClick={() => setPage((value) => value - 1)} type="button">이전</button>
                        {Array.from({ length: products.totalPages }, (_, index) => index + 1).slice(Math.max(0, page - 3), page + 2).map((number) => (
                            <button className={`h-9.5 min-w-9.5 border border-line ${page === number ? 'bg-ink text-white' : 'bg-transparent'}`} onClick={() => setPage(number)} key={number} type="button">{number}</button>
                        ))}
                        <button className="h-9.5 min-w-9.5 border border-line bg-transparent disabled:opacity-35" disabled={!products.hasNext} onClick={() => setPage((value) => value + 1)} type="button">다음</button>
                    </nav>
                )}
            </section>
        </>
    )
}

function StatusPanel({ title, description }: { title: string; description?: string }) {
    return <div className="grid min-h-80 place-content-center gap-2 text-center text-muted"><strong className="text-ink">{title}</strong>{description && <p className="m-0">{description}</p>}</div>
}
