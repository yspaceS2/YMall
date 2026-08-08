import { createRef } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { ProductSuggestion } from '../types/product'
import { StoreSearchPanel } from './StoreSearchPanel'

const suggestions: ProductSuggestion[] = [
    { productId: 1, name: '무선 키보드', thumbnailUrl: null, matchType: 'PREFIX' },
    { productId: 2, name: '기계식 키보드', thumbnailUrl: null, matchType: 'FUZZY' },
]

function renderPanel(overrides: Partial<Parameters<typeof StoreSearchPanel>[0]> = {}) {
    const props: Parameters<typeof StoreSearchPanel>[0] = {
        panelRef: createRef<HTMLDivElement>(),
        inputRef: createRef<HTMLInputElement>(),
        keyword: '키보드',
        normalizedKeyword: '키보드',
        suggestions,
        activeSuggestionIndex: -1,
        isLoading: false,
        hasError: false,
        shouldShowSuggestions: true,
        onKeywordChange: vi.fn(),
        onActiveSuggestionChange: vi.fn(),
        onSearch: vi.fn(),
        onClose: vi.fn(),
        ...overrides,
    }
    render(<StoreSearchPanel {...props} />)
    return props
}

describe('StoreSearchPanel', () => {
    it('입력 변경과 폼 검색을 상위 상태로 전달한다', async () => {
        const user = userEvent.setup()
        const props = renderPanel()

        fireEvent.change(screen.getByRole('combobox', { name: '상품 검색' }), {
            target: { value: '마우스' },
        })
        await user.click(screen.getByRole('button', { name: '상품 검색 실행' }))

        expect(props.onKeywordChange).toHaveBeenCalledWith('마우스')
        expect(props.onActiveSuggestionChange).toHaveBeenCalledWith(-1)
        expect(props.onSearch).toHaveBeenCalledWith('키보드')
    })

    it('키보드로 추천어를 이동하고 선택한다', () => {
        const props = renderPanel({ activeSuggestionIndex: 0 })
        const input = screen.getByRole('combobox', { name: '상품 검색' })

        fireEvent.keyDown(input, { key: 'ArrowDown' })
        expect(props.onActiveSuggestionChange).toHaveBeenCalledWith(1)

        fireEvent.keyDown(input, { key: 'Enter' })
        expect(props.onSearch).toHaveBeenCalledWith('무선 키보드')
    })

    it('추천 검색 오류와 닫기 동작을 표시한다', async () => {
        const user = userEvent.setup()
        const props = renderPanel({ suggestions: [], hasError: true })

        expect(screen.getByRole('status')).toHaveTextContent('추천 검색어를 불러오지 못했습니다.')
        await user.click(screen.getByRole('button', { name: '검색 닫기' }))
        expect(props.onClose).toHaveBeenCalledOnce()
    })
})
