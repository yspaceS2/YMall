import { useEffect, useState } from 'react'
import { getProductSuggestions } from '../api/products'
import type { ProductSuggestion } from '../types/product'

const SEARCH_DEBOUNCE_MS = 250

interface SuggestionResult {
    requestKey: string
    suggestions: ProductSuggestion[]
    error: boolean
}

export function useProductSearchSuggestions(
    keyword: string,
    enabled = true,
    categoryId?: number,
) {
    const normalizedKeyword = keyword.replace(/\s/g, '')
    const requestKey = `${categoryId ?? 'all'}:${normalizedKeyword}`
    const [result, setResult] = useState<SuggestionResult>({
        requestKey: '',
        suggestions: [],
        error: false,
    })
    const shouldSearch = enabled && normalizedKeyword.length >= 2

    useEffect(() => {
        if (!shouldSearch) return

        const controller = new AbortController()
        const timer = window.setTimeout(() => {
            getProductSuggestions(keyword.trim(), 8, categoryId, controller.signal)
                .then((suggestions) => {
                    setResult({ requestKey, suggestions, error: false })
                })
                .catch((error: unknown) => {
                    if (error instanceof Error && error.name === 'AbortError') return
                    setResult({ requestKey, suggestions: [], error: true })
                })
        }, SEARCH_DEBOUNCE_MS)

        return () => {
            window.clearTimeout(timer)
            controller.abort()
        }
    }, [categoryId, keyword, normalizedKeyword, requestKey, shouldSearch])

    const isCurrentResult = result.requestKey === requestKey

    return {
        normalizedKeyword,
        suggestions: shouldSearch && isCurrentResult ? result.suggestions : [],
        isLoading: shouldSearch && !isCurrentResult,
        hasError: shouldSearch && isCurrentResult && result.error,
        shouldShowSuggestions: shouldSearch,
    }
}
