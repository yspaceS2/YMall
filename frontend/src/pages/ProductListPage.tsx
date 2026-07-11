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
      <section className="catalog-hero">
        <p>CURATED FOR YOUR EVERYDAY</p>
        <h1>Discover your next favorite.</h1>
        <span>오래 곁에 둘 가치 있는 물건들을 소개합니다.</span>
      </section>

      <section className="catalog-section">
        <div className="catalog-heading">
          <div><span>SHOP</span><h2>전체 상품</h2><p>{products?.totalElements ?? 0}개의 상품</p></div>
          <form className="search-form" onSubmit={(event) => { event.preventDefault(); setQuery(keyword.trim()); setCategoryId(undefined); setPage(1) }}>
            <input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="찾고 있는 상품을 검색해보세요" aria-label="상품 검색" />
            <button type="submit" aria-label="검색"><Search /></button>
          </form>
        </div>

        <div className="category-bar">
          <div className="category-tabs">
            <button className={!categoryId && !query ? 'active' : ''} onClick={() => selectCategory()} type="button">ALL</button>
            {categories.map((category) => (
              <button className={categoryId === category.categoryId ? 'active' : ''} onClick={() => selectCategory(category.categoryId)} key={category.categoryId} type="button">{category.name}</button>
            ))}
          </div>
          <span><SlidersHorizontal /> 추천순</span>
        </div>

        {loading && <StatusPanel title="상품을 불러오는 중입니다" />}
        {!loading && error && <StatusPanel title="상품을 불러오지 못했습니다" description={error} />}
        {!loading && !error && products?.content.length === 0 && <StatusPanel title="조건에 맞는 상품이 없습니다" description="검색어나 카테고리를 변경해 보세요." />}
        {!loading && !error && products && products.content.length > 0 && (
          <div className="product-grid">{products.content.map((product) => <ProductCard product={product} key={product.productId} />)}</div>
        )}

        {products && products.totalPages > 1 && (
          <nav className="pagination" aria-label="상품 페이지">
            <button disabled={!products.hasPrevious} onClick={() => setPage((value) => value - 1)} type="button">이전</button>
            {Array.from({ length: products.totalPages }, (_, index) => index + 1).slice(Math.max(0, page - 3), page + 2).map((number) => (
              <button className={page === number ? 'active' : ''} onClick={() => setPage(number)} key={number} type="button">{number}</button>
            ))}
            <button disabled={!products.hasNext} onClick={() => setPage((value) => value + 1)} type="button">다음</button>
          </nav>
        )}
      </section>
    </>
  )
}

function StatusPanel({ title, description }: { title: string; description?: string }) {
  return <div className="status-panel"><strong>{title}</strong>{description && <p>{description}</p>}</div>
}
