import { Search, X } from 'lucide-react'
import type { FormEvent, RefObject } from 'react'
import type { ProductSuggestion } from '../types/product'

export function StoreSearchPanel({
    panelRef,
    inputRef,
    keyword,
    normalizedKeyword,
    suggestions,
    activeSuggestionIndex,
    isLoading,
    hasError,
    shouldShowSuggestions,
    onKeywordChange,
    onActiveSuggestionChange,
    onSearch,
    onClose,
}: {
    panelRef: RefObject<HTMLDivElement | null>
    inputRef: RefObject<HTMLInputElement | null>
    keyword: string
    normalizedKeyword: string
    suggestions: ProductSuggestion[]
    activeSuggestionIndex: number
    isLoading: boolean
    hasError: boolean
    shouldShowSuggestions: boolean
    onKeywordChange: (keyword: string) => void
    onActiveSuggestionChange: (index: number) => void
    onSearch: (keyword: string) => void
    onClose: () => void
}) {
    const submit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        onSearch(keyword)
    }

    return (
        <div ref={panelRef} className="absolute top-[calc(100%+8px)] right-4 z-50 w-[min(420px,calc(100vw-32px))] rounded-2xl border border-line bg-surface p-3 shadow-[0_18px_50px_rgba(20,20,16,.18)] min-[601px]:right-[clamp(24px,5vw,72px)]" id="store-search-panel">
            <form className="flex items-center gap-2" role="search" aria-label="통합 상품 검색" onSubmit={submit}>
                <div className="flex h-11 min-w-0 flex-1 items-center rounded-xl border border-ink bg-paper px-3">
                    <Search className="mr-2 size-4.5 shrink-0 text-muted" aria-hidden="true" />
                    <input
                        ref={inputRef}
                        className="min-w-0 flex-1 border-0 bg-transparent text-sm outline-none placeholder:text-muted"
                        value={keyword}
                        onChange={(event) => {
                            onKeywordChange(event.target.value)
                            onActiveSuggestionChange(-1)
                        }}
                        onKeyDown={(event) => {
                            if (event.key === 'Escape') {
                                onClose()
                                return
                            }
                            if (suggestions.length === 0) return
                            if (event.key === 'ArrowDown') {
                                event.preventDefault()
                                onActiveSuggestionChange((activeSuggestionIndex + 1) % suggestions.length)
                            } else if (event.key === 'ArrowUp') {
                                event.preventDefault()
                                onActiveSuggestionChange(activeSuggestionIndex <= 0
                                    ? suggestions.length - 1
                                    : activeSuggestionIndex - 1)
                            } else if (event.key === 'Enter' && activeSuggestionIndex >= 0) {
                                event.preventDefault()
                                onSearch(suggestions[activeSuggestionIndex].name)
                            }
                        }}
                        placeholder="찾고 싶은 상품을 검색해 보세요"
                        aria-label="상품 검색"
                        role="combobox"
                        aria-autocomplete="list"
                        aria-controls="product-search-suggestions"
                        aria-expanded={normalizedKeyword.length >= 2}
                        aria-activedescendant={activeSuggestionIndex >= 0
                            ? `product-search-suggestion-${suggestions[activeSuggestionIndex].productId}`
                            : undefined}
                    />
                    <button className="shrink-0 rounded-lg bg-ink px-3.5 py-2 text-xs font-extrabold text-paper" type="submit" aria-label="상품 검색 실행">검색</button>
                </div>
                <button className="inline-grid size-9 shrink-0 place-items-center rounded-full text-muted transition-colors hover:bg-paper hover:text-ink" type="button" aria-label="검색 닫기" onClick={onClose}>
                    <X className="size-4" aria-hidden="true" />
                </button>
            </form>
            {shouldShowSuggestions && (
                <div className="mt-2 overflow-hidden rounded-xl border border-line bg-paper" id="product-search-suggestions" role="listbox" aria-label="추천 검색어">
                    {isLoading && <p className="px-4 py-3 text-sm text-muted" role="status">추천 검색어를 찾는 중입니다.</p>}
                    {!isLoading && hasError && <p className="px-4 py-3 text-sm text-muted" role="status">추천 검색어를 불러오지 못했습니다.</p>}
                    {!isLoading && !hasError && suggestions.length === 0 && <p className="px-4 py-3 text-sm text-muted" role="status">일치하는 추천 검색어가 없습니다.</p>}
                    {!isLoading && suggestions.map((suggestion, index) => (
                        <button key={suggestion.productId} id={`product-search-suggestion-${suggestion.productId}`} className={`flex w-full items-center gap-3 px-3 py-2.5 text-left text-sm transition-colors ${index === activeSuggestionIndex ? 'bg-surface' : 'hover:bg-surface'}`} type="button" role="option" aria-selected={index === activeSuggestionIndex} onMouseEnter={() => onActiveSuggestionChange(index)} onClick={() => onSearch(suggestion.name)}>
                            {suggestion.thumbnailUrl ? <img className="size-10 shrink-0 rounded-lg object-cover" src={suggestion.thumbnailUrl} alt="" /> : <span className="grid size-10 shrink-0 place-items-center rounded-lg bg-surface text-muted"><Search className="size-4" aria-hidden="true" /></span>}
                            <span className="min-w-0 flex-1 truncate font-semibold">{suggestion.name}</span>
                            {suggestion.matchType === 'FUZZY' && <span className="shrink-0 text-[10px] font-bold text-muted">유사 검색</span>}
                        </button>
                    ))}
                </div>
            )}
        </div>
    )
}
